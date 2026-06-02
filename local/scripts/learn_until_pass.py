#!/usr/bin/env python3
"""
Re-ejecuta evaluacion hasta pasar el promotion gate o agotar iteraciones.
Todo bajo `local/`: benchmark, intent_eval.py, reports, train_augment.jsonl.

En cada fallo, anade filas a train_augment.jsonl tomadas del benchmark (ground truth)
para los casos que fallaron (intencion o entidades), y vuelve a evaluar con few-shot.

Limitacion: sin cambiar umbrales ni el modelo, puede estancarse; revisa local/reports/learning-progress.jsonl.
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List, Set


def load_eval_index(eval_path: Path) -> Dict[str, Dict[str, Any]]:
    by_id: Dict[str, Dict[str, Any]] = {}
    for line in eval_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        row = json.loads(line)
        by_id[row["id"]] = row
    return by_id


def collect_failure_rows(report: Dict[str, Any], eval_by_id: Dict[str, Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Construye filas de entrenamiento desde errores de intencion y de entidades."""
    out: List[Dict[str, Any]] = []
    seen_text: Set[str] = set()

    def add_row(case_id: str, *, kind: str) -> None:
        ev = eval_by_id.get(case_id)
        if not ev:
            return
        text = str(ev.get("text", "")).strip()
        if not text:
            return
        key = text.lower()
        if key in seen_text:
            return
        seen_text.add(key)
        row: Dict[str, Any] = {
            "text": text,
            "intent": ev["expectedIntent"],
            "entities": ev.get("expectedEntities") or {},
            "meta": {"source": "learn_until_pass", "kind": kind, "caseId": case_id},
        }
        out.append(row)

    for _flow, flow_data in report.get("byFlow", {}).items():
        for err in flow_data.get("errors", []) or []:
            add_row(str(err["id"]), kind="intent_mismatch")
        for err in flow_data.get("entityErrors", []) or []:
            add_row(str(err["id"]), kind="entity_mismatch")

    return out


def append_jsonl_unique(path: Path, rows: List[Dict[str, Any]], existing_keys: Set[str]) -> tuple[int, Set[str]]:
    path.parent.mkdir(parents=True, exist_ok=True)
    added = 0
    keys = set(existing_keys)
    mode = "a" if path.is_file() else "w"
    with path.open(mode, encoding="utf-8") as f:
        for row in rows:
            k = row["text"].strip().lower()
            if k in keys:
                continue
            f.write(json.dumps(row, ensure_ascii=False) + "\n")
            keys.add(k)
            added += 1
    return added, keys


def load_existing_keys(path: Path) -> Set[str]:
    if not path.is_file():
        return set()
    keys: Set[str] = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        row = json.loads(line)
        keys.add(str(row.get("text", "")).strip().lower())
    return keys


def append_progress(path: Path, record: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as f:
        f.write(json.dumps(record, ensure_ascii=False) + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Loop de eval hasta pasar gate (few-shot auto)")
    parser.add_argument("--session", default="learn", help="Prefijo de version (ej. learn-20260416)")
    parser.add_argument("--max-iterations", type=int, default=15)
    parser.add_argument("--provider", choices=["anthropic", "openai"], default="anthropic")
    parser.add_argument("--model", default="", help="Override; si vacio usa ANTHROPIC_MODEL o default del eval")
    parser.add_argument("--max-few-shot", type=int, default=48)
    parser.add_argument(
        "--reset-augment",
        action="store_true",
        help="Borra train_augment.jsonl antes de empezar (nueva sesion limpia).",
    )
    parser.add_argument(
        "--train-base",
        default="train_v1.jsonl",
        help="Archivo few-shot base en local/datasets/intents/ (ej. train_v2.jsonl).",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[2]
    eval_path = root / "local" / "datasets" / "intents" / "eval_v1.jsonl"
    run_script = root / "local" / "scripts" / "intent_eval.py"
    augment_path = root / "local" / "datasets" / "intents" / "train_augment.jsonl"
    progress_path = root / "local" / "reports" / "learning-progress.jsonl"
    session_slug = "".join(c if c.isalnum() or c in "-_" else "-" for c in args.session)

    if args.reset_augment and augment_path.is_file():
        augment_path.unlink()

    eval_by_id = load_eval_index(eval_path)
    existing_keys = load_existing_keys(augment_path)

    for i in range(1, args.max_iterations + 1):
        version = f"{session_slug}-i{i}"
        cmd: List[str] = [
            sys.executable,
            str(run_script),
            "--provider",
            args.provider,
            "--version",
            version,
            "--few-shot-augment",
            str(augment_path),
            "--max-few-shot",
            str(args.max_few_shot),
        ]
        if args.model:
            cmd.extend(["--model", args.model])
        train_base_path = root / "local" / "datasets" / "intents" / args.train_base
        cmd.extend(["--few-shot-base", str(train_base_path)])

        print(f"\n=== Iteracion {i}/{args.max_iterations}: {' '.join(cmd[2:])} ===\n", flush=True)
        proc = subprocess.run(cmd, cwd=str(root))
        report_file = root / "local" / "reports" / f"run-{version}.json"
        if not report_file.is_file():
            print("No se genero reporte; abortando.", file=sys.stderr)
            return 1
        report = json.loads(report_file.read_text(encoding="utf-8"))
        passed = report.get("promotionDecision", {}).get("result") == "APROBADO"
        summary = report.get("summary", {})
        failures = report.get("promotionGate", {}).get("failures", [])

        append_progress(
            progress_path,
            {
                "session": session_slug,
                "iteration": i,
                "version": version,
                "passed": passed,
                "exitCode": proc.returncode,
                "summary": summary,
                "failureCodes": [f.get("code") for f in failures],
                "augmentPath": str(augment_path.relative_to(root))
                if str(augment_path).startswith(str(root))
                else str(augment_path),
            },
        )

        if passed:
            print(f"\nGate APROBADO en iteracion {i}. Ver: {report_file}", flush=True)
            print(f"Progreso acumulado: {progress_path}", flush=True)
            return 0

        rows = collect_failure_rows(report, eval_by_id)
        added, existing_keys = append_jsonl_unique(augment_path, rows, existing_keys)
        print(
            f"\nIteracion {i}: gate RECHAZADO. Ejemplos nuevos en augment: {added} "
            f"(total unicos en augment conocidos: {len(existing_keys)})",
            flush=True,
        )

        if added == 0:
            print(
                "Sin nuevos ejemplos que añadir: el modelo puede estar estancado, "
                "el gate exige metricas no alcanzables solo con few-shot, o fallan casos no listados en errores. "
                "Opciones: sube el train base (train_v1 o train_v2), ajusta local/config/thresholds.json, cambia modelo o revisa local/datasets/intents/eval_v1.jsonl.",
                file=sys.stderr,
            )
            return 3

    print(f"\nMax iteraciones ({args.max_iterations}) sin pasar gate. Ver: {progress_path}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main())
