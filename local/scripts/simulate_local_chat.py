#!/usr/bin/env python3
"""
Simulador interactivo de chat local (sin online, sin mobile).

Uso (raiz del repo):
  python3 local/scripts/simulate_local_chat.py
  python3 local/scripts/simulate_local_chat.py --verbose
  python3 local/scripts/simulate_local_chat.py --save-transcript local/reports/chat-session.jsonl
"""

from __future__ import annotations

import argparse
import json
import math
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Tuple

import joblib

DEFAULT_CLARIFY_THRESHOLD = 0.35


def repo_root_from_here() -> Path:
    return Path(__file__).resolve().parents[2]


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
        return (
            "No me queda claro lo que necesitas. "
            "Puedes decirme si quieres consultar saldo, pagar tarjeta o hacer transferencia."
        )
    if action == "SHOW_BALANCE":
        return "Perfecto. Te ayudo con la consulta de saldo y movimientos recientes."
    if action == "SHOW_PAY_CARD_OPTIONS":
        return (
            "Entendido. Te ayudo con el pago de tarjeta. "
            "Indica la tarjeta y si deseas pago minimo, total o monto personalizado."
        )
    if action == "SHOW_TRANSFER_GUIDE":
        if intent == "TRANSFER_OWN_ACCOUNTS":
            return "Vamos con tu transferencia entre cuentas propias. Indica cuenta origen, destino y monto."
        return "Vamos con tu transferencia a terceros. Indica destinatario, banco y monto."
    if action == "SHOW_SUPPORT":
        return "Esto lo gestiona soporte. Te puedo derivar a web, telefono o WhatsApp."
    return "Te ayudo con eso. Dame un poco mas de detalle."


def append_jsonl(path: Path, row: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as f:
        f.write(json.dumps(row, ensure_ascii=False) + "\n")


def run_interactive(pipeline, clarify_threshold: float, verbose: bool, save_transcript: Path | None) -> None:
    print("Bot: Hola, soy tu asistente local. Escribe tu mensaje (Enter vacio para salir).")
    while True:
        try:
            user_text = input("Tu: ").strip()
        except EOFError:
            print()
            break
        if not user_text:
            break

        intent, confidence = predict_intent_with_confidence(pipeline, user_text)
        action = decide_next_action(intent, confidence, clarify_threshold)
        bot_text = build_bot_response(intent, action)

        print(f"Bot: {bot_text}")
        if verbose:
            print(f"     [intent={intent} confidence={confidence:.3f} action={action}]")

        if save_transcript is not None:
            now = datetime.now(timezone.utc).isoformat()
            append_jsonl(
                save_transcript,
                {
                    "recordedAt": now,
                    "userText": user_text,
                    "predictedIntent": intent,
                    "confidence": round(confidence, 4),
                    "nextAction": action,
                    "botText": bot_text,
                },
            )


def main() -> None:
    parser = argparse.ArgumentParser(description="Simulador interactivo de chat local")
    parser.add_argument("--repo-root", type=Path, default=None)
    parser.add_argument("--model", type=Path, default=None)
    parser.add_argument(
        "--clarify-threshold",
        type=float,
        default=DEFAULT_CLARIFY_THRESHOLD,
        help="Umbral para pedir aclaracion (default: 0.35)",
    )
    parser.add_argument("--verbose", action="store_true")
    parser.add_argument(
        "--save-transcript",
        type=Path,
        default=None,
        help="Guarda cada turno en JSONL",
    )
    args = parser.parse_args()

    root = args.repo_root or repo_root_from_here()
    model_path = args.model or (root / "local" / "models" / "intent_tfidf_svc.joblib")
    pipeline = load_pipeline(model_path)
    threshold = max(0.0, min(1.0, args.clarify_threshold))
    run_interactive(pipeline, threshold, args.verbose, args.save_transcript)


if __name__ == "__main__":
    main()
