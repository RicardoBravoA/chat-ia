#!/usr/bin/env bash
# Atajo: fusiona augment + errores en train_v1, backup, entrena sklearn.
# Opcional: paso previo con LLM (API key en entorno).
#
# Uso (raíz del repo):
#   ./local/scripts/finalize_intent_training.sh
#   ./local/scripts/finalize_intent_training.sh --with-llm
#   ./local/scripts/finalize_intent_training.sh --dry-run
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
PY="${ROOT}/local/venv/bin/python3"
[[ -x "$PY" ]] || PY=python3
exec "$PY" local/scripts/finalize_intent_training.py "$@"
