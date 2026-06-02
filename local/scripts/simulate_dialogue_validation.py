#!/usr/bin/env python3
"""
Valida aprendizaje conversacional local (sin online, sin mobile).

Evalua cada turno con dos objetivos:
1) expectedIntent: la intencion clasificada.
2) expectedNextAction: la accion conversacional esperada.

Uso (raiz del repo):
  python3 local/scripts/simulate_dialogue_validation.py
  python3 local/scripts/simulate_dialogue_validation.py --append-errors
"""

from __future__ import annotations

import argparse
import json
import math
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import joblib

DEFAULT_CLARIFY_THRESHOLD = 0.35


def repo_root_from_here() -> Path:
    return Path(__file__).resolve().parents[2]


def read_jsonl(path: Path) -> List[Dict[str, Any]]:
    if not path.is_file():
        return []
    out: List[Dict[str, Any]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line:
            out.append(json.loads(line))
    return out


def default_errors_path(root: Path) -> Path:
    return root / "local" / "datasets" / "intents" / "chat_errors.jsonl"


def append_error_records(path: Path, records: List[Dict[str, Any]]) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    existing_texts: set[str] = set()
    if path.is_file():
        for row in read_jsonl(path):
            text = row.get("text")
            if isinstance(text, str):
                existing_texts.add(text.strip())

    written = 0
    new_lines: List[str] = []
    for row in records:
        text = row.get("text")
        if not isinstance(text, str):
            continue
        key = text.strip()
        if not key or key in existing_texts:
            continue
        existing_texts.add(key)
        if "recordedAt" not in row:
            row = {**row, "recordedAt": datetime.now(timezone.utc).isoformat()}
        new_lines.append(json.dumps(row, ensure_ascii=False))
        written += 1

    if new_lines:
        with path.open("a", encoding="utf-8") as f:
            for line in new_lines:
                f.write(line + "\n")
    return written


def load_pipeline(model_path: Path):
    if not model_path.is_file():
        raise SystemExit(
            f"No existe {model_path}. Entrena primero:\n"
            "  python3 local/scripts/train_intent_classifier.py --eval"
        )
    artifact = joblib.load(model_path)
    return artifact["pipeline"]


def softmax(values: List[float]) -> List[float]:
    m = max(values)
    exps = [math.exp(v - m) for v in values]
    s = sum(exps)
    if s <= 0:
        return [0.0 for _ in values]
    return [v / s for v in exps]


def predict_intent_with_confidence(pipeline, text: str) -> Tuple[str, float]:
    intent = pipeline.predict([text])[0]
    scores = pipeline.decision_function([text])
    classes = list(pipeline.named_steps["clf"].classes_)

    if isinstance(scores[0], list) or getattr(scores, "ndim", 1) > 1:
        row = list(scores[0])
        probs = softmax([float(v) for v in row])
        confidence = max(probs) if probs else 0.0
        return intent, float(confidence)

    # Binario: decision_function devuelve margen escalar.
    margin = float(scores[0])
    confidence = 1.0 / (1.0 + math.exp(-abs(margin)))
    return intent, confidence


def decide_next_action(intent: str, confidence: float, clarify_threshold: float) -> str:
    if intent == "AMBIGUOUS" or confidence < clarify_threshold:
        return "ASK_CLARIFICATION"
    if intent == "CHECK_BALANCE":
        return "SHOW_BALANCE"
    if intent == "PAY_CREDIT_CARD":
        return "SHOW_PAY_CARD_OPTIONS"
    if intent in ("TRANSFER_OWN_ACCOUNTS", "TRANSFER_THIRD_PARTY"):
        return "SHOW_TRANSFER_GUIDE"
    if intent == "OUT_OF_SCOPE":
        return "SHOW_SUPPORT"
    return "GENERIC_REPLY"


def run_dialogue_eval(
    pipeline,
    eval_path: Path,
    clarify_threshold: float,
    append_errors: bool,
    errors_path: Optional[Path],
    no_fail: bool,
) -> int:
    rows = read_jsonl(eval_path)
    if not rows:
        print(f"No hay escenarios en {eval_path}")
        return 2

    total = 0
    intent_fail = 0
    action_fail = 0
    error_rows: List[Dict[str, Any]] = []

    for row in rows:
        sid = str(row.get("scenarioId", "?"))
        turn = int(row.get("turn", 0))
        text = row.get("userText", "")
        expected_intent = row.get("expectedIntent", "")
        expected_action = row.get("expectedNextAction", "")
        if not isinstance(text, str) or not isinstance(expected_intent, str) or not isinstance(expected_action, str):
            continue
        total += 1
        predicted_intent, confidence = predict_intent_with_confidence(pipeline, text)
        predicted_action = decide_next_action(predicted_intent, confidence, clarify_threshold)

        ok_intent = predicted_intent == expected_intent
        ok_action = predicted_action == expected_action
        ok = ok_intent and ok_action

        if not ok_intent:
            intent_fail += 1
            if append_errors and errors_path is not None:
                error_rows.append(
                    {
                        "text": text,
                        "intent": expected_intent,
                        "predictedIntent": predicted_intent,
                        "source": "dialogue_eval_fail",
                        "caseId": f"{sid}-T{turn}",
                        "entities": {},
                    }
                )
        if not ok_action:
            action_fail += 1

        mark = "OK " if ok else "BAD"
        print(
            f"{mark} {sid} T{turn} intent exp={expected_intent} got={predicted_intent} | "
            f"action exp={expected_action} got={predicted_action} | conf={confidence:.3f}"
        )
        if not ok:
            print(f"    text={text!r}")

    if append_errors and errors_path is not None and error_rows:
        n = append_error_records(errors_path, error_rows)
        print(f"\nAñadidas {n} fila(s) nuevas a {errors_path}")

    intent_acc = (total - intent_fail) / total if total else 0.0
    action_acc = (total - action_fail) / total if total else 0.0
    print("\nResumen")
    print(f"- Casos: {total}")
    print(f"- Intent accuracy: {intent_acc:.3f} (fallos={intent_fail})")
    print(f"- Next-action accuracy: {action_acc:.3f} (fallos={action_fail})")

    if (intent_fail > 0 or action_fail > 0) and not no_fail:
        return 1
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Valida conversacion local (intent + next action)")
    parser.add_argument("--repo-root", type=Path, default=None)
    parser.add_argument("--model", type=Path, default=None)
    parser.add_argument("--eval", type=Path, default=None)
    parser.add_argument("--append-errors", action="store_true")
    parser.add_argument("--errors-file", type=Path, default=None)
    parser.add_argument("--no-fail", action="store_true")
    parser.add_argument(
        "--clarify-threshold",
        type=float,
        default=DEFAULT_CLARIFY_THRESHOLD,
        help="Umbral de confianza para pedir aclaracion (default: 0.35)",
    )
    args = parser.parse_args()

    root = args.repo_root or repo_root_from_here()
    model_path = args.model or (root / "local" / "models" / "intent_tfidf_svc.joblib")
    eval_path = args.eval or (root / "local" / "datasets" / "intents" / "dialogue_eval_v1.jsonl")
    errors_path = args.errors_file or default_errors_path(root)

    pipeline = load_pipeline(model_path)
    code = run_dialogue_eval(
        pipeline=pipeline,
        eval_path=eval_path,
        clarify_threshold=max(0.0, min(1.0, args.clarify_threshold)),
        append_errors=args.append_errors,
        errors_path=errors_path if args.append_errors else None,
        no_fail=args.no_fail,
    )
    raise SystemExit(code)


if __name__ == "__main__":
    main()
