#!/usr/bin/env python3
"""
Simulador / validación del clasificador TF-IDF entrenado con train_intent_classifier.py.

- Por defecto: recorre eval_v1.jsonl y compara intención predicha vs expectedIntent.
- --interactive: prueba frases desde la consola.
- --message: una sola frase.

Uso (raíz del repo):
  python3 local/scripts/simulate_intent_validation.py
  python3 local/scripts/simulate_intent_validation.py --append-errors
  python3 local/scripts/simulate_intent_validation.py --interactive --record-chat
  python3 local/scripts/simulate_intent_validation.py -m "paga mi tc"
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional

import joblib


def repo_root_from_here() -> Path:
    return Path(__file__).resolve().parents[2]


def read_jsonl(path: Path) -> List[Dict[str, Any]]:
    rows: List[Dict[str, Any]] = []
    if not path.is_file():
        return rows
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line:
            rows.append(json.loads(line))
    return rows


def default_errors_path(root: Path) -> Path:
    return root / "local" / "datasets" / "intents" / "chat_errors.jsonl"


def append_error_records(path: Path, records: List[Dict[str, Any]]) -> int:
    """
    Añade líneas nuevas a chat_errors.jsonl sin duplicar el mismo texto (normalizado).
    Devuelve cuántas líneas se escribieron.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    existing: set[str] = set()
    if path.is_file():
        for r in read_jsonl(path):
            t = r.get("text")
            if isinstance(t, str):
                existing.add(t.strip())
    written = 0
    lines: List[str] = []
    for r in records:
        t = r.get("text")
        if not isinstance(t, str):
            continue
        key = t.strip()
        if not key or key in existing:
            continue
        existing.add(key)
        if "recordedAt" not in r:
            r = {**r, "recordedAt": datetime.now(timezone.utc).isoformat()}
        lines.append(json.dumps(r, ensure_ascii=False))
        written += 1
    if lines:
        with path.open("a", encoding="utf-8") as f:
            for line in lines:
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


def run_eval(
    pipeline,
    eval_path: Path,
    json_out: bool,
    no_fail: bool,
    append_errors: bool,
    errors_path: Optional[Path],
) -> int:
    rows = read_jsonl(eval_path)
    if not rows:
        print(f"No hay casos en {eval_path}", file=sys.stderr)
        return 2

    wrong = 0
    lines_out: List[str] = []
    results: List[Dict[str, Any]] = []
    to_append: List[Dict[str, Any]] = []

    for r in rows:
        eid = r.get("id", "?")
        text = r.get("text", "")
        exp = r.get("expectedIntent", "")
        if not isinstance(text, str) or not isinstance(exp, str):
            continue
        pred = pipeline.predict([text])[0]
        ok = pred == exp
        if not ok:
            wrong += 1
            if append_errors and errors_path is not None:
                to_append.append(
                    {
                        "text": text,
                        "intent": exp,
                        "predictedIntent": pred,
                        "source": "eval_fail",
                        "caseId": eid,
                        "entities": {},
                    }
                )
        rec = {"id": eid, "text": text, "expected": exp, "predicted": pred, "ok": ok}
        results.append(rec)
        if json_out:
            continue
        mark = "OK " if ok else "BAD"
        lines_out.append(f"{mark}  {eid}  expected={exp}  got={pred}")
        if not ok:
            lines_out.append(f"       text={text!r}")

    if append_errors and errors_path is not None and to_append:
        n = append_error_records(errors_path, to_append)
        if not json_out:
            print(f"\nAñadidas {n} fila(s) nuevas a {errors_path}", file=sys.stderr)

    if json_out:
        print(json.dumps({"eval_path": str(eval_path), "results": results}, ensure_ascii=False, indent=2))
    else:
        print(f"Casos: {len(results)}  Fallos: {wrong}  ({eval_path.name})\n")
        print("\n".join(lines_out))
        acc = (len(results) - wrong) / len(results) if results else 0.0
        print(f"\nAccuracy: {acc:.3f}")

    if wrong > 0 and not no_fail:
        return 1
    return 0


def run_interactive(
    pipeline,
    record_chat: bool,
    errors_path: Path,
) -> None:
    print("Escribe frases para clasificar (vacío o Ctrl-D para salir).")
    if record_chat:
        print("Si el intent predicho falla, escribe la intención correcta cuando se pida.")
    while True:
        try:
            line = input("> ").strip()
        except EOFError:
            print()
            break
        if not line:
            break
        pred = pipeline.predict([line])[0]
        print(f"  intent: {pred}")
        if record_chat:
            try:
                fix = input("  Corrección (intent correcto, vacío si OK): ").strip()
            except EOFError:
                print()
                break
            if fix:
                n = append_error_records(
                    errors_path,
                    [
                        {
                            "text": line,
                            "intent": fix,
                            "predictedIntent": pred,
                            "source": "interactive_chat",
                            "entities": {},
                        }
                    ],
                )
                if n:
                    print(f"  → guardado en {errors_path} ({n} fila nueva)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Valida el modelo de intención TF-IDF local")
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Raíz del repo (por defecto: inferida desde este script)",
    )
    parser.add_argument(
        "--model",
        type=Path,
        default=None,
        help="Ruta al .joblib (por defecto: local/models/intent_tfidf_svc.joblib)",
    )
    parser.add_argument(
        "--eval",
        type=Path,
        default=None,
        help="JSONL de evaluación (por defecto: local/datasets/intents/eval_v1.jsonl)",
    )
    parser.add_argument(
        "-m",
        "--message",
        default=None,
        help="Una frase; imprime solo la intención predicha",
    )
    parser.add_argument(
        "-i",
        "--interactive",
        action="store_true",
        help="Modo interactivo",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        dest="json_out",
        help="Salida JSON del benchmark completo",
    )
    parser.add_argument(
        "--no-fail",
        action="store_true",
        help="Siempre termina con código 0 aunque haya fallos en el benchmark",
    )
    parser.add_argument(
        "--append-errors",
        action="store_true",
        help="Tras el benchmark, añade fallos a chat_errors.jsonl (intención correcta incluida)",
    )
    parser.add_argument(
        "--errors-file",
        type=Path,
        default=None,
        help="Ruta a chat_errors.jsonl (por defecto: local/datasets/intents/chat_errors.jsonl)",
    )
    parser.add_argument(
        "--record-chat",
        action="store_true",
        help="Con --interactive: guarda correcciones en chat_errors.jsonl",
    )
    args = parser.parse_args()

    root = args.repo_root or repo_root_from_here()
    model_path = args.model or (root / "local" / "models" / "intent_tfidf_svc.joblib")
    eval_path = args.eval or (root / "local" / "datasets" / "intents" / "eval_v1.jsonl")
    errors_path = args.errors_file or default_errors_path(root)

    pipeline = load_pipeline(model_path)

    if args.message is not None:
        pred = pipeline.predict([args.message])[0]
        print(pred)
        return

    if args.interactive:
        run_interactive(pipeline, args.record_chat, errors_path)
        return

    code = run_eval(
        pipeline,
        eval_path,
        args.json_out,
        args.no_fail,
        args.append_errors,
        errors_path if args.append_errors else None,
    )
    raise SystemExit(code)


if __name__ == "__main__":
    main()
