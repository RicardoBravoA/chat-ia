#!/usr/bin/env python3
"""
Reproduce escenarios de conversación Woz y muestra respuesta del bot.

Uso (raíz del repo):
  python3 local/scripts/replay_dialogue_scenarios.py
  python3 local/scripts/replay_dialogue_scenarios.py --scenario S002
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Dict, List

_SCRIPTS = Path(__file__).resolve().parent
if str(_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS))

from woz_client import build_bot_response, classify_with_woz, decide_next_action  # noqa: E402

DEFAULT_CLARIFY_THRESHOLD = 0.55


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


def main() -> None:
    parser = argparse.ArgumentParser(description="Reproduce escenarios Woz y respuestas del bot")
    parser.add_argument("--repo-root", type=Path, default=None)
    parser.add_argument("--eval", type=Path, default=None)
    parser.add_argument("--scenario", default=None, help="Filtra por scenarioId")
    parser.add_argument("--clarify-threshold", type=float, default=DEFAULT_CLARIFY_THRESHOLD)
    args = parser.parse_args()

    root = args.repo_root or repo_root_from_here()
    eval_path = args.eval or (root / "local" / "datasets" / "intents" / "dialogue_eval_v1.jsonl")
    threshold = max(0.0, min(1.0, args.clarify_threshold))

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
        pred = classify_with_woz(text)
        action = decide_next_action(pred.intent, pred.confidence, threshold)
        bot = build_bot_response(pred.intent, action)

        if sid != current_scenario:
            current_scenario = sid
            print(f"\n=== {sid} ===")
        print(f"Turno {turn}")
        print(f"Usuario: {text}")
        print(f"Bot:     {bot}")
        print(
            f"Meta:    intent={expected_intent} action={expected_action} | "
            f"Pred: intent={pred.intent} action={action} conf={pred.confidence:.3f}"
        )


if __name__ == "__main__":
    main()
