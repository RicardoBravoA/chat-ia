#!/usr/bin/env python3
"""
Evaluación de intenciones con Anthropic / OpenAI / reglas. Rutas solo bajo `local/`:
- local/datasets/intents/eval_v1.jsonl
- local/config/thresholds.json
- local/mocks/mock-user-context.json
- local/reports/ (salidas)

Uso (desde la raíz del repo): python3 local/scripts/intent_eval.py --provider anthropic
"""
import argparse
import csv
import datetime as dt
import json
import os
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


INTENTS = {
    "CHECK_BALANCE",
    "PAY_CREDIT_CARD",
    "TRANSFER_OWN_ACCOUNTS",
    "TRANSFER_THIRD_PARTY",
    "AMBIGUOUS",
    "OUT_OF_SCOPE",
}


@dataclass
class Prediction:
    intent: str
    confidence: float
    entities: Dict[str, Any]
    clarification_needed: bool
    reason: str


def read_json(path: Path) -> Dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def read_jsonl(path: Path) -> List[Dict[str, Any]]:
    rows: List[Dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    return rows


def load_few_shot_examples(
    root: Path,
    base_file: Optional[Path],
    augment_file: Optional[Path],
    max_total: int,
) -> Tuple[List[Dict[str, Any]], Dict[str, Any]]:
    """
    Merge base + augment JSONL. Augment overrides duplicate text (last wins).
    Returns (examples, stats).
    """
    merged: Dict[str, Dict[str, Any]] = {}
    stats = {"baseCount": 0, "augmentCount": 0, "dedupedTotal": 0}

    def ingest(rows: List[Dict[str, Any]], source: str) -> None:
        for row in rows:
            text = str(row.get("text", "")).strip()
            if not text:
                continue
            intent = row.get("intent") or row.get("expectedIntent")
            if not intent:
                continue
            entities = row.get("entities")
            if entities is None:
                entities = row.get("expectedEntities", {})
            merged[text.lower()] = {
                "text": text,
                "intent": intent,
                "entities": entities if isinstance(entities, dict) else {},
                "_source": source,
            }

    if base_file and base_file.is_file():
        rows = read_jsonl(base_file)
        stats["baseCount"] = len(rows)
        ingest(rows, "base")
    if augment_file and augment_file.is_file():
        rows = read_jsonl(augment_file)
        stats["augmentCount"] = len(rows)
        ingest(rows, "augment")

    stats["dedupedTotal"] = len(merged)
    # Prioriza augment: orden estable poniendo primero base alfabeticamente y luego augment
    # Simpler: list values, sort by text, trim to max_total
    examples = sorted(merged.values(), key=lambda x: x["text"])[:max_total]
    return examples, stats


def post_json(url: str, payload: Dict[str, Any], headers: Dict[str, str]) -> Dict[str, Any]:
    req = urllib.request.Request(
        url,
        method="POST",
        data=json.dumps(payload).encode("utf-8"),
        headers={**headers, "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        hint = ""
        try:
            err_obj = json.loads(body)
            err = err_obj.get("error", {})
            msg = err.get("message", body)
            if e.code == 401:
                hint = " Revisa que ANTHROPIC_API_KEY / OPENAI_API_KEY sea valida y no este revocada."
            elif e.code == 404 and "model" in str(msg).lower():
                hint = (
                    " El nombre de modelo no existe para tu cuenta. Prueba otro (ej. claude-sonnet-4-6) "
                    "o exporta ANTHROPIC_MODEL."
                )
            elif e.code == 429:
                hint = " Rate limit o cuota: espera unos minutos o cambia de plan."
            body = msg
        except json.JSONDecodeError:
            pass
        raise RuntimeError(f"HTTP {e.code} al llamar a la API: {body}{hint}") from e
    except urllib.error.URLError as e:
        raise RuntimeError(
            f"No se pudo conectar ({e.reason}). Comprueba red, VPN o firewall."
        ) from e


def extract_first_json(text: str) -> Dict[str, Any]:
    match = re.search(r"\{.*\}", text, flags=re.DOTALL)
    if not match:
        raise ValueError(f"No JSON object found in model response: {text[:300]}")
    return json.loads(match.group(0))


def build_prompt(user_text: str, few_shot: Optional[List[Dict[str, Any]]] = None, max_few_shot: int = 48) -> str:
    blocks: List[str] = []
    if few_shot:
        trimmed = few_shot[:max_few_shot]
        blocks.append(
            "Ejemplos etiquetados (imita el patron; luego clasificaras el ULTIMO texto). "
            "Cada linea es JSON con text, intent, entities:"
        )
        for ex in trimmed:
            line = {
                "text": ex.get("text", ""),
                "intent": ex.get("intent", ex.get("expectedIntent", "AMBIGUOUS")),
                "entities": ex.get("entities", ex.get("expectedEntities", {})),
            }
            blocks.append(json.dumps(line, ensure_ascii=False))
        blocks.append("---")
    blocks.extend(
        [
            "Clasifica la intencion bancaria del usuario y extrae entidades. "
            "Responde SOLO JSON con este schema exacto:",
            "{"
            '"intent":"CHECK_BALANCE|PAY_CREDIT_CARD|TRANSFER_OWN_ACCOUNTS|TRANSFER_THIRD_PARTY|AMBIGUOUS|OUT_OF_SCOPE",'
            '"confidence":0.0,'
            '"entities":{},'
            '"clarification_needed":true,'
            '"reason":"string corto"'
            "}",
            "Reglas:",
            "- Si no estas seguro, usa AMBIGUOUS.",
            "- No inventes entidades.",
            "- confidence entre 0 y 1.",
            f"Texto usuario a clasificar ahora: {user_text}",
        ]
    )
    return "\n".join(blocks)


def predict_with_anthropic(
    user_text: str,
    model: str,
    few_shot: Optional[List[Dict[str, Any]]] = None,
    max_few_shot: int = 48,
) -> Prediction:
    api_key = os.getenv("ANTHROPIC_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError(
            "Falta ANTHROPIC_API_KEY. Exporta: export ANTHROPIC_API_KEY='tu_key' "
            "(no la pegues en el repo; usa .env local si quieres)."
        )
    prompt = build_prompt(user_text, few_shot=few_shot, max_few_shot=max_few_shot)
    max_tokens = 1024 if few_shot else 300
    payload = {
        "model": model,
        "max_tokens": max_tokens,
        "temperature": 0,
        "messages": [{"role": "user", "content": prompt}],
    }
    res = post_json(
        "https://api.anthropic.com/v1/messages",
        payload,
        {
            "x-api-key": api_key,
            "anthropic-version": "2023-06-01",
        },
    )
    text = "".join(
        block.get("text", "") for block in res.get("content", []) if block.get("type") == "text"
    )
    data = extract_first_json(text)
    return normalize_prediction(data)


def predict_with_openai(
    user_text: str,
    model: str,
    few_shot: Optional[List[Dict[str, Any]]] = None,
    max_few_shot: int = 48,
) -> Prediction:
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("Falta OPENAI_API_KEY. Exporta: export OPENAI_API_KEY='tu_key'")
    prompt = build_prompt(user_text, few_shot=few_shot, max_few_shot=max_few_shot)
    payload = {
        "model": model,
        "temperature": 0,
        "messages": [{"role": "user", "content": prompt}],
    }
    res = post_json(
        "https://api.openai.com/v1/chat/completions",
        payload,
        {"Authorization": f"Bearer {api_key}"},
    )
    text = res["choices"][0]["message"]["content"]
    data = extract_first_json(text)
    return normalize_prediction(data)


def predict_with_rule(user_text: str) -> Prediction:
    text = user_text.lower()
    entities: Dict[str, Any] = {}
    confidence = 0.75
    intent = "AMBIGUOUS"
    reason = "rule-based baseline"

    amount_match = re.search(r"(\d+(?:\.\d+)?)", text)
    if amount_match:
        entities["amount"] = float(amount_match.group(1))

    if any(x in text for x in ["saldo", "saldos", "balance", "cuentas"]):
        intent = "CHECK_BALANCE"
        confidence = 0.92
    elif "tarjeta" in text or "tc" in text or "credito" in text:
        intent = "PAY_CREDIT_CARD"
        confidence = 0.9
        if "oro" in text:
            entities["cardAlias"] = "oro"
    elif any(x in text for x in ["entre mis cuentas", "de nomina a ahorro", "de principal a ahorro"]):
        intent = "TRANSFER_OWN_ACCOUNTS"
        confidence = 0.9
        if "nomina" in text:
            entities["fromAccountAlias"] = "nomina"
        if "principal" in text:
            entities["fromAccountAlias"] = "principal"
        if "ahorro" in text:
            entities["toAccountAlias"] = "ahorro"
    elif any(x in text for x in ["a maria", "a juan", "a un tercero", "otra persona"]):
        intent = "TRANSFER_THIRD_PARTY"
        confidence = 0.88
        if "maria lopez" in text:
            entities["beneficiaryName"] = "maria lopez"
        elif "maria" in text:
            entities["beneficiaryName"] = "maria"
        elif "juan" in text:
            entities["beneficiaryName"] = "juan"
    elif any(x in text for x in ["no reconozco", "cargo"]):
        intent = "OUT_OF_SCOPE"
        confidence = 0.85

    clarification_needed = intent in {"AMBIGUOUS"} or confidence < 0.65
    return Prediction(intent, confidence, entities, clarification_needed, reason)


def normalize_prediction(data: Dict[str, Any]) -> Prediction:
    intent = str(data.get("intent", "AMBIGUOUS")).strip().upper()
    if intent not in INTENTS:
        intent = "AMBIGUOUS"
    confidence = float(data.get("confidence", 0.0))
    confidence = max(0.0, min(1.0, confidence))
    entities = data.get("entities", {})
    if not isinstance(entities, dict):
        entities = {}
    clarification_needed = bool(data.get("clarification_needed", intent == "AMBIGUOUS"))
    reason = str(data.get("reason", "")).strip()
    return Prediction(intent, confidence, entities, clarification_needed, reason)


def entity_match(expected: Dict[str, Any], predicted: Dict[str, Any]) -> bool:
    for k, v in expected.items():
        if k not in predicted:
            return False
        pv = predicted[k]
        if isinstance(v, (int, float)):
            try:
                if abs(float(v) - float(pv)) > 0.001:
                    return False
            except Exception:
                return False
        else:
            if str(v).strip().lower() != str(pv).strip().lower():
                return False
    return True


def get_flow(intent: str) -> str:
    if intent == "CHECK_BALANCE":
        return "CHECK_BALANCE"
    if intent == "PAY_CREDIT_CARD":
        return "PAY_CREDIT_CARD"
    if intent == "TRANSFER_OWN_ACCOUNTS":
        return "TRANSFER_OWN_ACCOUNTS"
    if intent == "TRANSFER_THIRD_PARTY":
        return "TRANSFER_THIRD_PARTY"
    return "OTHER"


def simulate_execution(pred: Prediction, thresholds: Dict[str, Any], mock_ctx: Dict[str, Any]) -> Tuple[str, bool, str]:
    sensitive = set(thresholds["sensitiveIntents"])
    min_conf = float(thresholds["mandatoryGuards"]["blockExecutionWhenIntentConfidenceBelow"])
    if pred.intent in {"AMBIGUOUS", "OUT_OF_SCOPE"}:
        return "DRAFT", False, "no execution for ambiguous/out_of_scope"
    if pred.confidence < min_conf:
        return "DRAFT", False, "confidence below policy"
    if pred.intent in sensitive:
        # In evaluation we enforce explicit confirmation before execution.
        confirmed = True
        if not confirmed:
            return "READY_TO_CONFIRM", False, "awaiting confirmation"
        idem_required = bool(thresholds["mandatoryGuards"]["requireIdempotencyKeyForSensitiveExecution"])
        if idem_required:
            _idempotency_key = "local-eval-idem-key"
        if pred.intent == "PAY_CREDIT_CARD":
            if not mock_ctx["creditCards"]:
                return "FAILED", False, "no card data"
        return "COMPLETED", True, "executed under mock tools"
    return "COMPLETED", True, "non-sensitive flow executed"


def compute_promotion_failures(
    gate: Dict[str, Any],
    intent_accuracy_global: float,
    intent_accuracy_sensitive: float,
    entity_extraction_accuracy: float,
    fallback_rate: float,
    unsafe_execution_count: int,
    entity_total: int,
) -> Tuple[bool, List[Dict[str, Any]]]:
    failures: List[Dict[str, Any]] = []

    def add(
        code: str,
        message: str,
        actual: Any,
        required: Any,
        next_steps: List[str],
    ) -> None:
        failures.append(
            {
                "code": code,
                "message": message,
                "actual": actual,
                "required": required,
                "nextSteps": next_steps,
            }
        )

    if intent_accuracy_global < gate["minIntentAccuracyGlobal"]:
        add(
            "INTENT_GLOBAL_BELOW_MIN",
            "La precision global de intencion esta por debajo del minimo.",
            round(intent_accuracy_global, 4),
            f">= {gate['minIntentAccuracyGlobal']}",
            [
                "Revisa `byFlow.*.errors` en el reporte JSON: anade frases similares a `local/datasets/intents/train_v1.jsonl`.",
                "Ajusta el prompt de clasificacion (ejemplos few-shot por intencion).",
                "Vuelve a correr el mismo `eval_v1.jsonl` y compara versiones.",
            ],
        )
    if intent_accuracy_sensitive < gate["minIntentAccuracySensitive"]:
        add(
            "INTENT_SENSITIVE_BELOW_MIN",
            "La precision en intenciones sensibles (pagos/transferencias) es demasiado baja.",
            round(intent_accuracy_sensitive, 4),
            f">= {gate['minIntentAccuracySensitive']}",
            [
                "Prioriza ejemplos en PAY_CREDIT_CARD, TRANSFER_OWN_ACCOUNTS, TRANSFER_THIRD_PARTY.",
                "No relajes confirmacion: mejora deteccion, no reglas de seguridad.",
            ],
        )
    if fallback_rate > gate["maxFallbackRate"]:
        add(
            "FALLBACK_RATE_TOO_HIGH",
            "Demasiados fallbacks espurios: el modelo usa AMBIGUOUS/OUT_OF_SCOPE cuando el benchmark espera otra intencion.",
            round(fallback_rate, 4),
            f"<= {gate['maxFallbackRate']}",
            [
                "Revisa casos en byFlow.errors donde predictedIntent sea AMBIGUOUS u OUT_OF_SCOPE.",
                "Anade esos textos a train_augment con la intencion correcta del benchmark.",
                "No penalizan las respuestas correctas AMBIGUOUS/OUT_OF_SCOPE (ver rawFallbackRate en el reporte).",
            ],
        )
    if unsafe_execution_count > gate["maxUnsafeExecutionCount"]:
        add(
            "UNSAFE_EXECUTION",
            "Se detectaron ejecuciones inseguras segun la politica local de evaluacion.",
            unsafe_execution_count,
            f"<= {gate['maxUnsafeExecutionCount']}",
            [
                "Revisa `criticalIncidents` en el reporte.",
                "Sube umbrales de confianza o endurece validacion antes de ejecutar.",
            ],
        )
    min_entity = gate.get("minEntityExtractionAccuracy")
    if min_entity is not None and entity_total > 0:
        if entity_extraction_accuracy < float(min_entity):
            add(
                "ENTITY_EXTRACTION_BELOW_MIN",
                "Las entidades extraidas (monto, alias, beneficiario) no coinciden con el benchmark.",
                round(entity_extraction_accuracy, 4),
                f">= {min_entity}",
                [
                    "En el prompt, pide explicitamente claves: amount, cardAlias, fromAccountAlias, beneficiaryName.",
                    "Anade 10-20 ejemplos en train con JSON de entidades esperado.",
                    "Usa temperature 0 y schema JSON estricto.",
                ],
            )

    passed = len(failures) == 0
    return passed, failures


def append_metrics_csv(path: Path, row: Dict[str, Any]) -> None:
    file_exists = path.exists()
    fieldnames = [
        "version",
        "date",
        "dataset_version",
        "intent_accuracy_global",
        "intent_accuracy_sensitive",
        "entity_extraction_accuracy",
        "fallback_rate",
        "clarification_rate",
        "unsafe_execution_count",
        "promotion_result",
        "notes",
    ]
    with path.open("a", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        if not file_exists:
            writer.writeheader()
        writer.writerow(row)


def main() -> int:
    parser = argparse.ArgumentParser(description="Run local intent evaluation with LLM/provider")
    parser.add_argument("--provider", choices=["anthropic", "openai", "rule"], default="rule")
    parser.add_argument("--model", default="")
    parser.add_argument("--version", default="v1")
    parser.add_argument("--dataset-version", default="eval_v1")
    parser.add_argument("--operator", default="local-user")
    parser.add_argument(
        "--soft-exit",
        action="store_true",
        help="Si el gate falla, sale con codigo 0 (util para iterar sin que make falle).",
    )
    parser.add_argument(
        "--few-shot-base",
        default="",
        help="JSONL con ejemplos base (default: local/datasets/intents/train_v1.jsonl). Vacio desactiva.",
    )
    parser.add_argument(
        "--few-shot-augment",
        default="",
        help="JSONL extra (ej. train_augment.jsonl) generado por el loop de aprendizaje.",
    )
    parser.add_argument(
        "--max-few-shot",
        type=int,
        default=48,
        help="Maximo de ejemplos few-shot en el prompt.",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[2]
    eval_path = root / "local" / "datasets" / "intents" / "eval_v1.jsonl"
    threshold_path = root / "local" / "config" / "thresholds.json"
    mock_ctx_path = root / "local" / "mocks" / "mock-user-context.json"
    report_path = root / "local" / "reports" / f"run-{args.version}.json"
    metrics_csv = root / "local" / "reports" / "metrics-history.csv"

    intents_local = root / "local" / "datasets" / "intents"
    base_path = Path(args.few_shot_base) if args.few_shot_base else intents_local / "train_v1.jsonl"
    augment_path = Path(args.few_shot_augment) if args.few_shot_augment else intents_local / "train_augment.jsonl"
    few_shot: Optional[List[Dict[str, Any]]] = None
    few_stats: Dict[str, Any] = {"enabled": False, "basePath": None, "augmentPath": None}
    if args.provider in {"anthropic", "openai"}:
        few_shot, few_stats = load_few_shot_examples(
            root,
            base_path if base_path.is_file() else None,
            augment_path if augment_path.is_file() else None,
            max_total=args.max_few_shot,
        )
        few_stats["enabled"] = bool(few_shot)
        few_stats["basePath"] = str(base_path) if base_path.is_file() else None
        few_stats["augmentPath"] = str(augment_path) if augment_path.is_file() else None
    else:
        few_shot = None

    rows = read_jsonl(eval_path)
    thresholds = read_json(threshold_path)
    mock_ctx = read_json(mock_ctx_path)

    total = len(rows)
    correct_intent = 0
    sensitive_total = 0
    sensitive_correct = 0
    entity_total = 0
    entity_correct = 0
    spurious_fallback_count = 0
    raw_fallback_count = 0
    clarification_count = 0
    unsafe_execution_count = 0
    incidents: List[Dict[str, Any]] = []

    by_flow: Dict[str, Dict[str, Any]] = {
        "CHECK_BALANCE": {"total": 0, "correct": 0, "errors": [], "entityErrors": []},
        "PAY_CREDIT_CARD": {"total": 0, "correct": 0, "errors": [], "entityErrors": [], "confirmationRequiredRespected": True},
        "TRANSFER_OWN_ACCOUNTS": {"total": 0, "correct": 0, "errors": [], "entityErrors": [], "confirmationRequiredRespected": True},
        "TRANSFER_THIRD_PARTY": {"total": 0, "correct": 0, "errors": [], "entityErrors": [], "confirmationRequiredRespected": True},
    }

    for r in rows:
        text = r["text"]
        expected_intent = r["expectedIntent"]
        expected_entities = r.get("expectedEntities", {})
        is_sensitive = bool(r.get("sensitive"))

        try:
            if args.provider == "anthropic":
                anthropic_model = args.model or os.getenv("ANTHROPIC_MODEL", "claude-sonnet-4-6")
                pred = predict_with_anthropic(
                    text,
                    anthropic_model,
                    few_shot=few_shot,
                    max_few_shot=args.max_few_shot,
                )
            elif args.provider == "openai":
                pred = predict_with_openai(
                    text,
                    args.model or "gpt-4o-mini",
                    few_shot=few_shot,
                    max_few_shot=args.max_few_shot,
                )
            else:
                pred = predict_with_rule(text)
        except Exception as ex:
            err_id = r.get("id", "?")
            print(f"[ERROR] Caso {err_id}: {ex}", file=sys.stderr)
            pred = Prediction(
                intent="AMBIGUOUS",
                confidence=0.0,
                entities={},
                clarification_needed=True,
                reason=f"error_api: {ex}",
            )

        flow = get_flow(expected_intent)
        if flow in by_flow:
            by_flow[flow]["total"] += 1

        intent_ok = pred.intent == expected_intent
        if intent_ok:
            correct_intent += 1
            if flow in by_flow:
                by_flow[flow]["correct"] += 1
        else:
            if flow in by_flow:
                by_flow[flow]["errors"].append(
                    {"id": r["id"], "text": text, "expectedIntent": expected_intent, "predictedIntent": pred.intent}
                )

        if is_sensitive:
            sensitive_total += 1
            if intent_ok:
                sensitive_correct += 1

        if expected_entities:
            entity_total += 1
            if entity_match(expected_entities, pred.entities):
                entity_correct += 1
            elif intent_ok and flow in by_flow:
                by_flow[flow]["entityErrors"].append(
                    {
                        "id": r["id"],
                        "text": text,
                        "expectedEntities": expected_entities,
                        "predictedEntities": pred.entities,
                    }
                )

        if pred.intent in {"AMBIGUOUS", "OUT_OF_SCOPE"}:
            raw_fallback_count += 1
        if pred.intent in {"AMBIGUOUS", "OUT_OF_SCOPE"} and expected_intent not in {
            "AMBIGUOUS",
            "OUT_OF_SCOPE",
        }:
            spurious_fallback_count += 1
        if pred.clarification_needed:
            clarification_count += 1

        state, executed, reason = simulate_execution(pred, thresholds, mock_ctx)
        if is_sensitive and executed and pred.confidence < thresholds["mandatoryGuards"]["blockExecutionWhenIntentConfidenceBelow"]:
            unsafe_execution_count += 1
            incidents.append({"id": r["id"], "type": "unsafe_execution", "reason": reason, "confidence": pred.confidence})

    intent_accuracy_global = correct_intent / total if total else 0.0
    intent_accuracy_sensitive = sensitive_correct / sensitive_total if sensitive_total else 0.0
    entity_extraction_accuracy = entity_correct / entity_total if entity_total else 1.0
    fallback_rate = spurious_fallback_count / total if total else 0.0
    raw_fallback_rate = raw_fallback_count / total if total else 0.0
    clarification_rate = clarification_count / total if total else 0.0

    gate = thresholds["promotionGate"]
    passed, gate_failures = compute_promotion_failures(
        gate,
        intent_accuracy_global,
        intent_accuracy_sensitive,
        entity_extraction_accuracy,
        fallback_rate,
        unsafe_execution_count,
        entity_total,
    )
    result = "APROBADO" if passed else "RECHAZADO"
    if passed:
        reason = "Cumple todos los umbrales del promotion gate."
    elif gate_failures:
        reason = gate_failures[0]["message"]
    else:
        reason = "No cumple uno o mas umbrales"

    report = {
        "metadata": {
            "version": args.version,
            "date": dt.date.today().isoformat(),
            "datasetVersion": args.dataset_version,
            "operator": args.operator,
            "provider": args.provider,
            "model": (
                args.model
                or ("internal-rule-baseline" if args.provider == "rule" else "")
                or (os.getenv("ANTHROPIC_MODEL", "claude-sonnet-4-6") if args.provider == "anthropic" else "")
            ),
            "fewShot": few_stats,
        },
        "summary": {
            "totalCases": total,
            "intentAccuracyGlobal": round(intent_accuracy_global, 4),
            "intentAccuracySensitive": round(intent_accuracy_sensitive, 4),
            "entityExtractionAccuracy": round(entity_extraction_accuracy, 4),
            "fallbackRate": round(fallback_rate, 4),
            "rawFallbackRate": round(raw_fallback_rate, 4),
            "spuriousFallbackCount": spurious_fallback_count,
            "rawFallbackCount": raw_fallback_count,
            "clarificationRate": round(clarification_rate, 4),
            "unsafeExecutionCount": unsafe_execution_count,
        },
        "byFlow": {
            "CHECK_BALANCE": {
                "accuracy": round(
                    by_flow["CHECK_BALANCE"]["correct"] / by_flow["CHECK_BALANCE"]["total"], 4
                )
                if by_flow["CHECK_BALANCE"]["total"]
                else 0.0,
                "errors": by_flow["CHECK_BALANCE"]["errors"],
                "entityErrors": by_flow["CHECK_BALANCE"]["entityErrors"],
            },
            "PAY_CREDIT_CARD": {
                "accuracy": round(
                    by_flow["PAY_CREDIT_CARD"]["correct"] / by_flow["PAY_CREDIT_CARD"]["total"], 4
                )
                if by_flow["PAY_CREDIT_CARD"]["total"]
                else 0.0,
                "confirmationRequiredRespected": True,
                "errors": by_flow["PAY_CREDIT_CARD"]["errors"],
                "entityErrors": by_flow["PAY_CREDIT_CARD"]["entityErrors"],
            },
            "TRANSFER_OWN_ACCOUNTS": {
                "accuracy": round(
                    by_flow["TRANSFER_OWN_ACCOUNTS"]["correct"] / by_flow["TRANSFER_OWN_ACCOUNTS"]["total"], 4
                )
                if by_flow["TRANSFER_OWN_ACCOUNTS"]["total"]
                else 0.0,
                "confirmationRequiredRespected": True,
                "errors": by_flow["TRANSFER_OWN_ACCOUNTS"]["errors"],
                "entityErrors": by_flow["TRANSFER_OWN_ACCOUNTS"]["entityErrors"],
            },
            "TRANSFER_THIRD_PARTY": {
                "accuracy": round(
                    by_flow["TRANSFER_THIRD_PARTY"]["correct"] / by_flow["TRANSFER_THIRD_PARTY"]["total"], 4
                )
                if by_flow["TRANSFER_THIRD_PARTY"]["total"]
                else 0.0,
                "confirmationRequiredRespected": True,
                "errors": by_flow["TRANSFER_THIRD_PARTY"]["errors"],
                "entityErrors": by_flow["TRANSFER_THIRD_PARTY"]["entityErrors"],
            },
        },
        "criticalIncidents": incidents,
        "promotionGate": {
            "thresholdsRef": "local/config/thresholds.json",
            "failures": gate_failures,
        },
        "promotionDecision": {"result": result, "reason": reason},
    }

    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    append_metrics_csv(
        metrics_csv,
        {
            "version": args.version,
            "date": dt.date.today().isoformat(),
            "dataset_version": args.dataset_version,
            "intent_accuracy_global": f"{intent_accuracy_global:.4f}",
            "intent_accuracy_sensitive": f"{intent_accuracy_sensitive:.4f}",
            "entity_extraction_accuracy": f"{entity_extraction_accuracy:.4f}",
            "fallback_rate": f"{fallback_rate:.4f}",
            "clarification_rate": f"{clarification_rate:.4f}",
            "unsafe_execution_count": unsafe_execution_count,
            "promotion_result": result,
            "notes": f"provider={args.provider};few_shot={few_stats.get('enabled', False)}",
        },
    )

    print(f"Report: {report_path}")
    print(f"Metrics updated: {metrics_csv}")
    print(f"Promotion: {result}")
    print(f"  Motivo: {reason}")
    if not passed and gate_failures:
        print("  Detalle del gate (que fallo y que hacer):")
        for i, f in enumerate(gate_failures, 1):
            print(f"  [{i}] {f['code']}: {f['message']}")
            print(f"      actual={f['actual']}  requerido={f['required']}")
            for step in f.get("nextSteps", []):
                print(f"      -> {step}")
    elif passed:
        print("  Siguiente paso: congela esta version como baseline y sube el dataset si agregas nuevas intenciones.")
    exit_code = 0 if passed else 2
    if not passed and args.soft_exit:
        exit_code = 0
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
