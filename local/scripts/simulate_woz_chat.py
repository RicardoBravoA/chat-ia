#!/usr/bin/env python3
"""
Simulador interactivo de chat Woz (LLM local vía Ollama, sin mobile).

Uso (raíz del repo):
  python3 local/scripts/simulate_woz_chat.py
  python3 local/scripts/simulate_woz_chat.py --verbose
  python3 local/scripts/simulate_woz_chat.py --save-transcript local/reports/chat-session.jsonl
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict

_SCRIPTS = Path(__file__).resolve().parent
if str(_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS))

from woz_client import build_bot_response, classify_with_woz, decide_next_action  # noqa: E402

DEFAULT_CLARIFY_THRESHOLD = 0.55


def repo_root_from_here() -> Path:
    return Path(__file__).resolve().parents[2]


def append_jsonl(path: Path, row: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as f:
        f.write(json.dumps(row, ensure_ascii=False) + "\n")


def run_interactive(clarify_threshold: float, verbose: bool, save_transcript: Path | None) -> None:
    print("Woz: Hola, soy tu copiloto bancario. Escribe tu mensaje (Enter vacío para salir).")
    while True:
        try:
            user_text = input("Tu: ").strip()
        except EOFError:
            print()
            break
        if not user_text:
            break

        pred = classify_with_woz(user_text)
        action = decide_next_action(pred.intent, pred.confidence, clarify_threshold)
        bot_text = build_bot_response(pred.intent, action)

        print(f"Woz: {bot_text}")
        if verbose:
            print(f"     [intent={pred.intent} confidence={pred.confidence:.3f} action={action}]")

        if save_transcript is not None:
            append_jsonl(
                save_transcript,
                {
                    "recordedAt": datetime.now(timezone.utc).isoformat(),
                    "userText": user_text,
                    "predictedIntent": pred.intent,
                    "confidence": round(pred.confidence, 4),
                    "nextAction": action,
                    "botText": bot_text,
                    "source": "woz",
                },
            )


def main() -> None:
    parser = argparse.ArgumentParser(description="Simulador interactivo de chat Woz")
    parser.add_argument(
        "--clarify-threshold",
        type=float,
        default=DEFAULT_CLARIFY_THRESHOLD,
        help="Umbral para pedir aclaracion (default: 0.55)",
    )
    parser.add_argument("--verbose", action="store_true")
    parser.add_argument("--save-transcript", type=Path, default=None)
    args = parser.parse_args()
    threshold = max(0.0, min(1.0, args.clarify_threshold))
    run_interactive(threshold, args.verbose, args.save_transcript)


if __name__ == "__main__":
    main()
