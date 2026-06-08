#!/usr/bin/env python3
"""
Clasifica intención con Woz (LLM local vía Ollama).

Uso (desde la raíz del repo):
  python3 local/scripts/predict_intent_woz.py "paga mi tc"
  echo "cuanto tengo" | python3 local/scripts/predict_intent_woz.py
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

_SCRIPTS = Path(__file__).resolve().parent
if str(_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS))

from woz_client import classify_with_woz  # noqa: E402


def main() -> None:
    parser = argparse.ArgumentParser(description="Predice intención con Woz (Ollama local)")
    parser.add_argument("text", nargs="?", default=None, help="Frase del usuario (stdin si falta)")
    parser.add_argument("--json", action="store_true", help="Salida JSON completa")
    args = parser.parse_args()

    if args.text is not None:
        text = args.text
    else:
        text = sys.stdin.read().strip()
    if not text:
        raise SystemExit("Frase vacía")

    pred = classify_with_woz(text)
    if args.json:
        print(
            json.dumps(
                {
                    "intent": pred.intent,
                    "confidence": pred.confidence,
                    "entities": pred.entities,
                    "clarification_needed": pred.clarification_needed,
                    "reason": pred.reason,
                    "source": pred.source,
                },
                ensure_ascii=False,
            )
        )
    else:
        print(pred.intent)


if __name__ == "__main__":
    main()
