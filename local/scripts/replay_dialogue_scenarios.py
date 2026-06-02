#!/usr/bin/env python3
"""
Reproduce escenarios de conversacion local y muestra respuesta del bot.

Uso (raiz del repo):
  python3 local/scripts/replay_dialogue_scenarios.py
  python3 local/scripts/replay_dialogue_scenarios.py --scenario S002
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
from typing import Any, Dict, List, Tuple

import joblib

DEFAULT_CLARIFY_THRESHOLD = 0.35


def repo_root_from_here() -> Path:
    return Path(__file__).resolve().parents[2]


def read_jsonl(path: Path) -> List[Dict[str, Any]]:
    if not path.is_file():
        return []
    rows: List[Dict[str, Any]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line:
            rows.append(json.loads(line))
    return rows


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
    if isinstance(scores[0], list) or getattr(scores, "ndim", 1) > 1:
        probs = softmax([float(v) for v in list(scores[0])])
        confidence = max(probs) if probs else 0.0
        return intent, float(confidence)
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


def build_bot_response(intent: str, action: str) -> str:
    if action == "ASK_CLARIFICATION":
        return "No me queda claro. Dime si quieres saldo, pago de tarjeta o transferencia."
    if action == "SHOW_BALANCE":
        return "Listo, te ayudo con saldo y movimientos."
    if action == "SHOW_PAY_CARD_OPTIONS":
        return "Perfecto, vamos con pago de tarjeta. Dime tarjeta y tipo de pago."
    if action == "SHOW_TRANSFER_GUIDE":
        return "Perfecto, indícame origen, destino y monto para la transferencia."
    if action == "SHOW_SUPPORT":
        return "Este tema lo maneja soporte. Te derivo por los canales disponibles."
    return "Te ayudo con eso. Cuéntame más."


def main() -> None:
    parser = argparse.ArgumentParser(description="Reproduce escenarios y respuestas del bot local")
    parser.add_argument("--repo-root", type=Path, default=None)
    parser.add_argument("--model", type=Path, default=None)
    parser.add_argument("--eval", type=Path, default=None)
    parser.add_argument("--scenario", default=None, help="Filtra por scenarioId")
    parser.add_argument("--clarify-threshold", type=float, default=DEFAULT_CLARIFY_THRESHOLD)
    args = parser.parse_args()

    root = args.repo_root or repo_root_from_here()
    model_path = args.model or (root / "local" / "models" / "intent_tfidf_svc.joblib")
    eval_path = args.eval or (root / "local" / "datasets" / "intents" / "dialogue_eval_v1.jsonl")
    threshold = max(0.0, min(1.0, args.clarify_threshold))

    pipeline = load_pipeline(model_path)
    rows = read_jsonl(eval_path)
    if not rows:
        raise SystemExit(f"No hay escenarios en {eval_path}")

    current_scenario = None
    for row in rows:
        sid = str(row.get("scenarioId", "?"))
        if args.scenario and sid != args.scenario:
            continue
        turn = int(row.get("turn", 0))
        text = str(row.get("userText", ""))
        expected_intent = str(row.get("expectedIntent", ""))
        expected_action = str(row.get("expectedNextAction", ""))
        intent, confidence = predict_intent_with_confidence(pipeline, text)
        action = decide_next_action(intent, confidence, threshold)
        bot = build_bot_response(intent, action)

        if sid != current_scenario:
            current_scenario = sid
            print(f"\n=== {sid} ===")
        print(f"Turno {turn}")
        print(f"Usuario: {text}")
        print(f"Bot:     {bot}")
        print(
            f"Meta:    intent={expected_intent} action={expected_action} | "
            f"Pred: intent={intent} action={action} conf={confidence:.3f}"
        )


if __name__ == "__main__":
    main()
