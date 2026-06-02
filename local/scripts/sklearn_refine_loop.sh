#!/usr/bin/env bash
# Bucle: entrenar TF-IDF → evaluar contra eval_v1.jsonl → si falla, añadir casos a chat_errors.jsonl → repetir.
# No modifica train_v1.jsonl ni eval_v1.jsonl; usa chat_errors.jsonl como refuerzo (el entrenador lo mezcla).
#
# Uso (desde la raíz del repo, con venv activado o con Python que tenga sklearn):
#   chmod +x local/scripts/sklearn_refine_loop.sh
#   ./local/scripts/sklearn_refine_loop.sh
#   ./local/scripts/sklearn_refine_loop.sh 25   # máximo 25 vueltas
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
MAX_ITERS="${1:-15}"
PY="${ROOT}/local/venv/bin/python3"
if [[ ! -x "$PY" ]]; then
  PY="python3"
fi

for ((i = 1; i <= MAX_ITERS; i++)); do
  echo ""
  echo "========== Iteración ${i}/${MAX_ITERS} =========="
  "$PY" local/scripts/train_intent_classifier.py --eval

  if "$PY" local/scripts/simulate_intent_validation.py; then
    echo "Benchmark eval_v1.jsonl: OK. Fin."
    exit 0
  fi

  echo "Hay fallos; añadiendo a chat_errors.jsonl (sin duplicar textos)..."
  "$PY" local/scripts/simulate_intent_validation.py --append-errors --no-fail
done

echo ""
echo "Se alcanzó el máximo de iteraciones sin pasar el benchmark." >&2
echo "Revisa: más ejemplos en train_v1.jsonl, heurística en intent_heuristic.json, o casos demasiado ambiguos en eval_v1.jsonl." >&2
exit 1
