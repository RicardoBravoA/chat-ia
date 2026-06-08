# Llevar Woz a otra laptop

Checklist para reproducir **Woz** (LLM local vía Ollama) y validar benchmarks.

## 1. Clonar repo

```bash
git clone <url> ia && cd ia
```

## 2. Instalar Ollama (oficial, no Homebrew en macOS)

En **macOS**, evita `brew install ollama` para modelos GGUF (`qwen2.5`): falta `llama-server` → HTTP 500.

```bash
brew uninstall ollama 2>/dev/null || true
curl -fsSL https://ollama.com/install.sh | sh
# o https://ollama.com/download
ollama pull qwen2.5:7b-instruct
ollama list
```

## 3. Probar clasificación

Desde la raíz del repo:

```bash
python3 local/scripts/predict_intent_woz.py "quiero pagar la tarjeta"
python3 local/scripts/simulate_intent_validation.py
```

## 4. Backend con Woz

```bash
cd backend && ./gradlew :api:run
```

Variables opcionales: `WOZ_MODEL`, `WOZ_OLLAMA_BASE_URL`, `INTENT_ROUTER_MODE=auto`.

## 5. Qué no va en Git (regenerable o local)

| Path | Notas |
|------|-------|
| `local/venv/` | Opcional (scripts usan stdlib) |
| `local/reports/` | Salida de `intent_eval.py` (gitignored) |
| `local/models/` | Obsoleto (sklearn); no usar |

Los datasets `eval_v1.jsonl`, `dialogue_eval_v1.jsonl`, `train_v1.jsonl` y `chat_errors.jsonl` **sí** van en Git.

**Runtime del backend:** solo necesita `local/config/intent_heuristic.json` (fallback JVM) además de Ollama.
