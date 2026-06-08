# Datasets de intenciones — Woz (`local/datasets/intents/`)

Objetivo: **validar y mejorar** el router **Woz** (LLM local vía Ollama) de forma repetible: ampliar ejemplos de referencia → benchmark → registrar errores en `chat_errors.jsonl` → re-validar.

`chat_errors.jsonl` **sí se versiona en Git** para compartir correcciones entre el equipo.

**Comandos:** desde la **raíz del repo** (`ia/`).

## Ciclo recomendado

1. **Ampliar ejemplos** — Paráfrasis en `train_v1.jsonl` o correcciones en `chat_errors.jsonl` (referencia humana; Woz no entrena pesos offline).
2. **Benchmark intención** — `python3 local/scripts/simulate_intent_validation.py` (requiere Ollama).
3. **Benchmark conversación** — `python3 local/scripts/simulate_dialogue_validation.py`.
4. **Chat interactivo** — `python3 local/scripts/simulate_woz_chat.py --verbose`.
5. **Volcar fallos** — `python3 local/scripts/simulate_intent_validation.py --append-errors`.
6. **Promotion gate** — `python3 local/scripts/intent_eval.py --provider woz --version woz-v1`.

```text
  eval_v1.jsonl / dialogue_eval_v1.jsonl
              ↓
     Woz (Ollama) + woz_client.py
              ↓
  simulate_*_validation.py  →  ¿error? → chat_errors.jsonl → commit
              ↓
  Ajustar WozPrompt.kt / woz_client.py / intent_heuristic.json
```

## Archivos en Git

| Archivo | Rol |
|---------|-----|
| `eval_v1.jsonl` | Benchmark fijo por frase (regresión). |
| `dialogue_eval_v1.jsonl` | Benchmark multi-turn (intención + next action). |
| `train_v1.jsonl` | Ejemplos de referencia / paráfrasis para enriquecer el dominio. |
| `chat_errors.jsonl` | Frases mal clasificadas + intención correcta. |

Intenciones válidas: `CHECK_BALANCE`, `PAY_CREDIT_CARD`, `TRANSFER_OWN_ACCOUNTS`, `TRANSFER_THIRD_PARTY`, `AMBIGUOUS`, `OUT_OF_SCOPE`.

## Mejorar Woz cuando falla el benchmark

1. Añade la frase fallida a `chat_errors.jsonl` con la `intent` correcta.
2. Añade **paráfrasis** similares a `train_v1.jsonl` (documentación del dominio).
3. Re-ejecuta `simulate_intent_validation.py`.
4. Si persiste, ajusta el system prompt en `woz_client.py` / `WozPrompt.kt`, `intent_heuristic.json` (fallback JVM) o el modelo (`WOZ_MODEL`).

La heurística JVM (`local/config/intent_heuristic.json`) actúa como **fallback** si Woz falla o tiene baja confianza (`INTENT_ROUTER_MODE=auto`).

## Formato JSONL

**Benchmark (`eval_v1.jsonl`):**

```json
{"id":"E001","text":"...","expectedIntent":"CHECK_BALANCE","expectedEntities":{},"sensitive":false}
```

**Corrección (`chat_errors.jsonl`):**

```json
{"text":"...","intent":"PAY_CREDIT_CARD","predictedIntent":"AMBIGUOUS","source":"eval_fail","entities":{}}
```
