#!/usr/bin/env python3
"""
Carga el modelo entrenado con train_intent_classifier.py y predice la intención.

Uso (desde la raíz del repo):
  python3 local/scripts/predict_intent.py "cuanto dinero tengo"
  echo "paga la tarjeta oro" | python3 local/scripts/predict_intent.py
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import joblib


def repo_root_from_here() -> Path:
    return Path(__file__).resolve().parents[2]


def main() -> None:
    parser = argparse.ArgumentParser(description="Predice intención con modelo TF-IDF local")
    parser.add_argument(
        "text",
        nargs="?",
        default=None,
        help="Frase del usuario (si falta, lee stdin)",
    )
    parser.add_argument(
        "--model",
        type=Path,
        default=None,
        help="Ruta al .joblib (por defecto: local/models/intent_tfidf_svc.joblib)",
    )
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=None,
    )
    args = parser.parse_args()
    root = args.repo_root or repo_root_from_here()
    model_path = args.model or (root / "local" / "models" / "intent_tfidf_svc.joblib")
    if not model_path.is_file():
        raise SystemExit(
            f"No existe {model_path}. Entrena primero: python3 local/scripts/train_intent_classifier.py"
        )

    artifact = joblib.load(model_path)
    pipeline = artifact["pipeline"]

    if args.text is not None:
        text = args.text
    else:
        text = sys.stdin.read().strip()
    if not text:
        raise SystemExit("Frase vacía")

    intent = pipeline.predict([text])[0]
    print(intent)


if __name__ == "__main__":
    main()
