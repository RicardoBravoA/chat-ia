# Entorno local — Woz (LLM local vía Ollama)

**Woz** es el copiloto bancario conversacional: clasifica intenciones y extrae entidades
con un **LLM local** (Ollama), sin tokens en nube ni pipeline sklearn/`.joblib`.

Todo Python, benchmarks y caché viven bajo `local/`.

**Checklist portable:** [llevar-a-otra-laptop.md](llevar-a-otra-laptop.md).

**Desde qué carpeta ejecutar:** los comandos `python3 local/scripts/...` asumen la **raíz del repo** (`ia/`).

## Requisitos previos

- **macOS / Linux** con Python 3.10+ (`python3 --version`). No hace falta venv ni `pip install`.
- **[Ollama](https://ollama.com)** instalado con el **instalador oficial** (recomendado en macOS).

### Instalar Ollama (macOS)

> **No uses `brew install ollama` en macOS** si vas a correr modelos GGUF (`qwen2.5`, `llama3`, etc.).
> Desde Ollama 0.30, la fórmula Homebrew **no incluye `llama-server`** y obtienes HTTP 500
> (`llama-server binary not found`). Ver [Homebrew#285917](https://github.com/Homebrew/homebrew-core/issues/285917).

**Opción recomendada — instalador oficial:**

```bash
# Si ya instalaste con Homebrew, quítalo primero:
brew uninstall ollama

# Instalador oficial (CLI + app):
curl -fsSL https://ollama.com/install.sh | sh

# Alternativa: descarga la app desde https://ollama.com/download
# (abre Ollama una vez; el servicio queda en segundo plano)
```

Comprueba que el CLI responde:

```bash
which ollama   # debe apuntar a .../Ollama.app/Contents/Resources/ollama
ollama --version
ls /Applications/Ollama.app   # debe existir aunque el install.sh muestre un error al final
```

Si el script oficial termina con `Unable to find application named 'Ollama'` pero **`/Applications/Ollama.app` existe**, la instalación **sí funcionó** — solo falló el paso de abrir la app automáticamente. Arranca manualmente:

```bash
open -a Ollama
# o en terminal:
ollama serve
curl -s http://127.0.0.1:11434/   # "Ollama is running"
```

Si tenías `ollama serve` de Homebrew en otra terminal, deténlo (Ctrl+C) antes de usar el binario oficial.

### Modelo Woz

```bash
ollama pull qwen2.5:7b-instruct
ollama list   # debe listar qwen2.5:7b-instruct
```

Si el servidor no está activo:

```bash
ollama serve   # o abre la app Ollama en macOS
```

### Diagnóstico rápido

```bash
python3 local/scripts/predict_intent_woz.py "paga mi tc"
```

## Variables de entorno (Woz)

| Variable | Default | Descripción |
|----------|---------|-------------|
| `WOZ_OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Base URL de Ollama |
| `WOZ_MODEL` | `qwen2.5:7b-instruct` | Modelo en Ollama |
| `WOZ_TIMEOUT_SECONDS` | `60` | Timeout HTTP |

El backend Ktor usa las mismas variables (ver [backend/README.md](../backend/README.md)).

## Probar Woz

Desde la **raíz del repo**:

```bash
python3 local/scripts/predict_intent_woz.py "paga mi tc"
python3 local/scripts/predict_intent_woz.py --json "cuanto tengo en cuenta"
python3 local/scripts/simulate_woz_chat.py --verbose
```

## Benchmarks (obligatorio antes de merge si tocáis routing)

```bash
python3 local/scripts/simulate_intent_validation.py
python3 local/scripts/simulate_dialogue_validation.py
python3 local/scripts/intent_eval.py --provider woz --version woz-v1
```

- `eval_v1.jsonl` — intención por frase (benchmark fijo).
- `dialogue_eval_v1.jsonl` — multi-turn (intención + next action).
- Fallos etiquetados → `chat_errors.jsonl` (versionado en Git).

## Intenciones válidas

`CHECK_BALANCE`, `PAY_CREDIT_CARD`, `TRANSFER_OWN_ACCOUNTS`, `TRANSFER_THIRD_PARTY`, `AMBIGUOUS`, `OUT_OF_SCOPE`

Dataset canónico: `local/datasets/intents/` (ver [datasets/intents/README.md](datasets/intents/README.md)).

## Estructura

```
local/
  README.md
  llevar-a-otra-laptop.md
  requirements.txt          # sin deps obligatorias (stdlib)
  env.sh.example            # plantilla de variables (opcional)
  scripts/
    woz_client.py           # cliente Ollama compartido
    predict_intent_woz.py   # CLI clasificación
    simulate_intent_validation.py
    simulate_dialogue_validation.py
    simulate_woz_chat.py      # chat interactivo
    replay_dialogue_scenarios.py
    intent_eval.py          # eval + promotion gate (woz / rule / cloud opcional)
  config/
    intent_heuristic.json   # fallback JVM (backend en modo auto)
    thresholds.json         # umbrales de intent_eval.py
  datasets/intents/
    eval_v1.jsonl           # benchmark regresión
    dialogue_eval_v1.jsonl  # benchmark multi-turn
    train_v1.jsonl          # ejemplos de referencia
    chat_errors.jsonl       # correcciones del equipo
  mocks/
    mock-user-context.json  # solo intent_eval.py (simulación)
  reports/                  # generado por intent_eval.py (gitignored)
```

### Qué necesita producción

| Componente | Archivo / servicio |
|------------|-------------------|
| Clasificador primario (modo `auto`) | **Heurística JVM** fast path → **Ollama/Woz** si no hay match claro |
| Fallback backend | `config/intent_heuristic.json` (también tras fallo/duda de Woz) |
| Python en runtime | **No** — solo para benchmarks en dev |

## Relación con el backend

- Chat SDUI: `WS /v1/chat/ws` → `BuildChatUiUseCase` → **cascade** heurística → Woz → heurística (`CascadeIntentClassifier` en modo `auto`).
- Legacy: `POST /v1/chat/route` (solo clasificación).
- Variables router: `INTENT_ROUTER_MODE` (`auto` \| `woz` \| `heuristic`), `INTENT_HEURISTIC_FAST_PATH_MIN_CONFIDENCE`, `WOZ_*`.

Woz **no ejecuta pagos** ni consulta saldos: solo clasifica. Montos y SDUI vienen de use cases + MongoDB.

## Evaluación cloud (opcional, solo dev)

Comparar Woz vs baseline sin Ollama:

```bash
python3 local/scripts/intent_eval.py --provider rule --version rule-baseline
```

Anthropic/OpenAI siguen disponibles en `intent_eval.py` para comparativas puntuales (requieren API key).
