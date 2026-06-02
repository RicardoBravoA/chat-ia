#!/usr/bin/env python3
"""
Fusiona fuentes extra en train_v1.jsonl, hace backup, ejecuta entrenamiento sklearn.

Fuentes (en orden de prioridad al solapar el mismo texto, gana la última):
  1) train_v1.jsonl (base)
  2) train_augment.jsonl
  3) chat_errors.jsonl (solo líneas con intent etiquetado; suelen ser correcciones)

Opcional: antes de fusionar, ejecuta learn_until_pass.py (Anthropic/OpenAI) para
rellenar train_augment desde fallos del eval vía API.

Uso (raíz del repo):
  python3 local/scripts/finalize_intent_training.py
  python3 local/scripts/finalize_intent_training.py --dry-run
  python3 local/scripts/finalize_intent_training.py --with-llm --llm-provider anthropic
  python3 local/scripts/finalize_intent_training.py --clear-augment
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional


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


def write_jsonl(path: Path, rows: List[Dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        for r in rows:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")


def norm_key(text: str) -> str:
    return text.strip().lower()


def canonical_row(r: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    text = r.get("text")
    intent = r.get("intent")
    if not isinstance(text, str) or not isinstance(intent, str):
        return None
    intent = intent.strip()
    if not intent or not text.strip():
        return None
    ent = r.get("entities")
    if not isinstance(ent, dict):
        ent = {}
    return {"text": text.strip(), "intent": intent, "entities": ent}


def rows_from_labeled_errors(path: Path) -> List[Dict[str, Any]]:
    out: List[Dict[str, Any]] = []
    for r in read_jsonl(path):
        row = canonical_row(r)
        if row:
            out.append(row)
    return out


def merge_rows(
    base: List[Dict[str, Any]],
    overlays: List[List[Dict[str, Any]]],
) -> List[Dict[str, Any]]:
    """Última capa gana para el mismo texto normalizado."""
    by_key: Dict[str, Dict[str, Any]] = {}
    for r in base:
        c = canonical_row(r)
        if c:
            by_key[norm_key(c["text"])] = c
    for layer in overlays:
        for r in layer:
            c = canonical_row(r)
            if c:
                by_key[norm_key(c["text"])] = c
    out = list(by_key.values())
    out.sort(key=lambda x: (x["intent"], x["text"].lower()))
    return out


def backup_file(path: Path, backups_dir: Path) -> Path:
    backups_dir.mkdir(parents=True, exist_ok=True)
    ts = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    dest = backups_dir / f"{path.name}.{ts}.bak"
    dest.write_bytes(path.read_bytes())
    return dest


def run_learn_until_pass(
    root: Path,
    provider: str,
    session: str,
    max_iter: int,
) -> int:
    script = root / "local" / "scripts" / "learn_until_pass.py"
    cmd = [
        sys.executable,
        str(script),
        "--provider",
        provider,
        "--session",
        session,
        "--max-iterations",
        str(max_iter),
    ]
    print(f"\n>>> {' '.join(cmd)}\n", flush=True)
    return subprocess.call(cmd, cwd=str(root))


def run_train_classifier(root: Path) -> int:
    script = root / "local" / "scripts" / "train_intent_classifier.py"
    cmd = [sys.executable, str(script), "--eval"]
    print(f"\n>>> {' '.join(cmd)}\n", flush=True)
    return subprocess.call(cmd, cwd=str(root))


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Fusiona augment/errors en train_v1, backup y entrena sklearn.",
    )
    parser.add_argument("--repo-root", type=Path, default=None)
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Solo muestra conteos; no escribe train_v1 ni entrena.",
    )
    parser.add_argument(
        "--no-backup",
        action="store_true",
        help="No crea backup de train_v1 antes de sobrescribir.",
    )
    parser.add_argument(
        "--clear-augment",
        action="store_true",
        help="Borra train_augment.jsonl tras fusionar con éxito.",
    )
    parser.add_argument(
        "--with-llm",
        action="store_true",
        help="Antes de fusionar, ejecuta learn_until_pass.py (requiere API key).",
    )
    parser.add_argument("--llm-provider", choices=["anthropic", "openai"], default="anthropic")
    parser.add_argument("--llm-session", default="finalize-local")
    parser.add_argument("--max-llm-iterations", type=int, default=15)
    parser.add_argument(
        "--no-train",
        action="store_true",
        help="Solo fusiona; no ejecuta train_intent_classifier.py.",
    )
    args = parser.parse_args()
    root = args.repo_root or repo_root_from_here()
    intents_dir = root / "local" / "datasets" / "intents"
    train_path = intents_dir / "train_v1.jsonl"
    aug_path = intents_dir / "train_augment.jsonl"
    err_path = intents_dir / "chat_errors.jsonl"
    backups_dir = intents_dir / ".backups"

    if args.with_llm:
        code = run_learn_until_pass(root, args.llm_provider, args.llm_session, args.max_llm_iterations)
        if code != 0:
            print(
                f"learn_until_pass terminó con código {code}. "
                "Continúo con la fusión si hay datos locales.",
                file=sys.stderr,
            )

    if not train_path.is_file():
        print(f"No existe {train_path}", file=sys.stderr)
        return 1

    base_rows = read_jsonl(train_path)
    aug_rows = read_jsonl(aug_path)
    err_rows = rows_from_labeled_errors(err_path)

    merged = merge_rows(base_rows, [aug_rows, err_rows])

    n_base = len([canonical_row(r) for r in base_rows if canonical_row(r)])
    n_merged = len(merged)
    print(
        f"Filas base válidas (train_v1): {n_base}  |  "
        f"augment: {len(aug_rows)}  |  errors etiquetados: {len(err_rows)}  "
        f"|  resultado tras deduplicar: {n_merged}",
        flush=True,
    )

    if args.dry_run:
        print("[dry-run] No se escribe disco ni se entrena.")
        return 0

    if not args.no_backup:
        bak = backup_file(train_path, backups_dir)
        print(f"Backup: {bak}", flush=True)

    write_jsonl(train_path, merged)
    print(f"Actualizado: {train_path} ({n_merged} líneas)", flush=True)

    if args.clear_augment and aug_path.is_file():
        aug_path.write_text("", encoding="utf-8")
        print(f"Vaciado: {aug_path}", flush=True)

    if args.no_train:
        return 0

    code = run_train_classifier(root)
    if code != 0:
        print(f"train_intent_classifier.py falló con código {code}", file=sys.stderr)
        return code

    print("\nListo: train_v1 fusionado, modelo en local/models/intent_tfidf_svc.joblib", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
