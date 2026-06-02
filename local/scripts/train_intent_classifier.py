#!/usr/bin/env python3
"""
Entrena un clasificador de intención local (TF-IDF + SVM) desde local/datasets/intents/train_v1.jsonl.
Compatible con Python 3.14 — solo depende de scikit-learn (ver local/requirements.txt).

Esto NO es fine-tuning de un LLM: son pesos pequeños en disco para enrutar intenciones.

Fuentes de datos: local/datasets/intents/train_v1.jsonl + train_augment.jsonl + opcionalmente
train_supplement.jsonl y chat_errors.jsonl (misma carpeta)
(ver local/datasets/intents/README.md).

Uso (desde la raíz del repo):
  python3 local/scripts/train_intent_classifier.py
  python3 local/scripts/train_intent_classifier.py --eval
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Dict, List, Tuple

import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import accuracy_score, classification_report
from sklearn.pipeline import Pipeline
from sklearn.svm import LinearSVC


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


def rows_from_labeled_errors(path: Path) -> List[Dict[str, Any]]:
    """Líneas en chat_errors.jsonl con intent explícito (oro para reentrenar)."""
    out: List[Dict[str, Any]] = []
    for r in read_jsonl(path):
        intent = r.get("intent")
        text = r.get("text")
        if not isinstance(text, str) or not isinstance(intent, str):
            continue
        intent = intent.strip()
        if not intent:
            continue
        out.append(
            {
                "text": text.strip(),
                "intent": intent,
                "entities": r.get("entities") if isinstance(r.get("entities"), dict) else {},
            }
        )
    return out


def load_training_rows(root: Path) -> Tuple[List[str], List[str]]:
    intents_local = root / "local" / "datasets" / "intents"
    train_path = intents_local / "train_v1.jsonl"
    aug_path = intents_local / "train_augment.jsonl"
    sup_path = intents_local / "train_supplement.jsonl"
    err_path = intents_local / "chat_errors.jsonl"
    rows = (
        read_jsonl(train_path)
        + read_jsonl(aug_path)
        + read_jsonl(sup_path)
        + rows_from_labeled_errors(err_path)
    )
    if not rows:
        raise SystemExit(
            f"No hay ejemplos en {train_path}, {aug_path}, {sup_path} ni errores etiquetados en {err_path}"
        )
    texts: List[str] = []
    labels: List[str] = []
    for r in rows:
        t = r.get("text")
        y = r.get("intent")
        if not isinstance(t, str) or not isinstance(y, str):
            continue
        texts.append(t)
        labels.append(y)
    if not texts:
        raise SystemExit("Ninguna fila válida con campos text/intent")
    return texts, labels


def load_eval_rows(path: Path) -> Tuple[List[str], List[str]]:
    rows = read_jsonl(path)
    texts: List[str] = []
    expected: List[str] = []
    for r in rows:
        t = r.get("text")
        y = r.get("expectedIntent")
        if isinstance(t, str) and isinstance(y, str):
            texts.append(t)
            expected.append(y)
    return texts, expected


def build_pipeline() -> Pipeline:
    return Pipeline(
        [
            (
                "tfidf",
                TfidfVectorizer(
                    ngram_range=(1, 2),
                    min_df=1,
                    sublinear_tf=True,
                ),
            ),
            (
                "clf",
                LinearSVC(class_weight="balanced", dual="auto", random_state=42),
            ),
        ]
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Entrena clasificador TF-IDF + LinearSVC desde JSONL")
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=None,
        help="Raíz del repo (por defecto: dos niveles arriba de este script)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Ruta del artefacto .joblib (por defecto: local/models/intent_tfidf_svc.joblib)",
    )
    parser.add_argument(
        "--eval",
        action="store_true",
        help="Tras entrenar, evalúa en local/datasets/intents/eval_v1.jsonl",
    )
    args = parser.parse_args()
    root = args.repo_root or repo_root_from_here()
    out = args.output or (root / "local" / "models" / "intent_tfidf_svc.joblib")

    texts, labels = load_training_rows(root)
    pipeline = build_pipeline()
    pipeline.fit(texts, labels)

    out.parent.mkdir(parents=True, exist_ok=True)
    artifact = {
        "pipeline": pipeline,
        "intents": sorted(set(labels)),
        "kind": "tfidf_linearsvc_v1",
        "train_examples": len(texts),
    }
    joblib.dump(artifact, out)
    print(f"Guardado: {out} ({len(texts)} ejemplos, {len(artifact['intents'])} intenciones)")

    if args.eval:
        eval_path = root / "local" / "datasets" / "intents" / "eval_v1.jsonl"
        ev_texts, ev_y = load_eval_rows(eval_path)
        if not ev_texts:
            print(f"Aviso: sin casos en {eval_path}", file=sys.stderr)
            return
        pred = pipeline.predict(ev_texts)
        acc = accuracy_score(ev_y, pred)
        print(f"Accuracy en eval_v1.jsonl: {acc:.3f}")
        print(classification_report(ev_y, pred, zero_division=0))


if __name__ == "__main__":
    main()
