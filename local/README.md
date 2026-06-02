# Entorno local (Mac) — instalación y datos

Todo lo relacionado con **Python**, **modelos**, **benchmark de intenciones** y **caché** de entrenamiento en esta máquina debe vivir **solo** bajo `local/`.

**Otra laptop (checklist portable):** [llevar-a-otra-laptop.md](llevar-a-otra-laptop.md).

**Desde qué carpeta ejecutar:** Los comandos `python3 local/scripts/...` asumen que estás en la **raíz del repo** (`ia/`, donde coexisten `local/`). Si tu terminal está en `local/`, la ruta queda duplicada (`local/local/scripts/...`). En ese caso usa `python3 scripts/train_intent_classifier.py --eval` (sin el prefijo `local/`), o bien `cd ..` a la raíz.

## Requisitos previos

- **macOS** con **Python** (`python3 --version`).
- **Xcode Command Line Tools** (si falta algo al compilar): `xcode-select --install`.
- Conexión a internet la **primera vez** (descarga de paquetes y modelos).

### Versión de Python (importante)

| Objetivo | Python recomendado |
|----------|-------------------|
| Solo **numpy + scikit-learn** (clasificador TF-IDF / ML clásico) | **3.10–3.14** (el `requirements.txt` base instala) |
| **sentence-transformers** (embeddings) | **3.11 o 3.12** — *PyTorch* aún no publica ruedas estables para **3.14**, por eso `pip` falla con *"no matching distributions ... torch"* |

Si tu Mac tiene solo Python 3.14, instala 3.12 con Homebrew y crea el venv con esa versión:

```bash
brew install python@3.12
/opt/homebrew/opt/python@3.12/bin/python3.12 -m venv local/venv
source local/venv/bin/activate
pip install -r local/requirements.txt
pip install -r local/requirements-embeddings.txt
```

(Intel Mac: la ruta puede ser `/usr/local/opt/python@3.12/...`.)

### Si `pip` falla al instalar `torch`

Si ves algo como:

```text
ERROR: No matching distribution found for torch>=2.2.0 (from versions: none)
```

estás usando un Python para el que **no hay** paquetes de PyTorch (hoy suele ser **3.14**). No es un fallo de red ni de versión en el archivo: hay que **crear otro venv con 3.11 o 3.12** (pasos de la tabla de arriba) y volver a instalar. Con 3.14 puedes seguir usando solo `requirements.txt` (TF-IDF / sklearn) sin embeddings neuronales.

## Pasos de instalación

Desde la **raíz del repositorio** (`ia/`):

### 1. Crear entorno virtual solo dentro de `local/`

```bash
cd /ruta/a/ia
python3 -m venv local/venv
```

### 2. Activar el entorno

```bash
source local/venv/bin/activate
```

(En Windows sería `local\venv\Scripts\activate`.)

### 3. Actualizar pip (recomendado)

```bash
python -m pip install --upgrade pip
```

### 4. Instalar dependencias

**Siempre** instala primero el núcleo:

Desde la **raíz del repo** (`ia/`):

```bash
pip install -r local/requirements.txt
```

Si ya estás **dentro** de `local/`:

```bash
pip install -r requirements.txt
```

**Embeddings** (opcional, con Python 3.11/3.12 y venv activo):

```bash
pip install -r requirements-embeddings.txt
```

### 5. (Opcional) Forzar caché de Hugging Face dentro de `local/`

Así los modelos de `sentence-transformers` no llenan tu home por defecto:

```bash
export HF_HOME="$PWD/local/.cache/huggingface"
export TRANSFORMERS_CACHE="$HF_HOME"
```

Puedes copiar `local/env.sh.example` a `local/env.sh`, ajustar si hace falta y ejecutar:

```bash
source local/env.sh
```

O añadir las mismas variables al final de `local/venv/bin/activate`.

### 6. Comprobar instalación

Solo núcleo (sin embeddings):

```bash
python -c "import sklearn, numpy; print('OK')"
```

Si instalaste embeddings (venv con Python 3.11/3.12):

```bash
python -c "import sklearn; import sentence_transformers; print('OK')"
```

## Entrenar un clasificador de intención en disco (Python 3.14, sin PyTorch)

Desde la **raíz del repo** (`ia/`), con el venv activado (`source local/venv/bin/activate`) y dependencias instaladas (`pip install -r local/requirements.txt`):

```bash
python3 local/scripts/train_intent_classifier.py --eval
```

- Sin `--eval` solo guarda el modelo; con `--eval` además muestra métricas sobre `eval_v1.jsonl`.
- Si estás en la carpeta `local/`, usa: `python3 scripts/train_intent_classifier.py --eval`.

### Qué modelo se usa

El script `local/scripts/train_intent_classifier.py` entrena un **pipeline de scikit-learn** (ML clásico sobre texto). **No** es un LLM ni fine-tuning de Claude/OpenAI: son pesos pequeños en disco para **clasificar la intención** a partir del texto del usuario.

| Componente | Rol |
|------------|-----|
| **`TfidfVectorizer`** | Convierte cada frase en vectores de frecuencia TF-IDF. Usa **n-gramas (1, 2)** (palabras sueltas y pares), `min_df=1`, `sublinear_tf=True`. |
| **`LinearSVC`** | Clasificador de máximo margen (SVM lineal). `class_weight="balanced"` para compensar clases con distinto número de ejemplos, `dual="auto"`, `random_state=42`. |

El artefacto guardado es un **`Pipeline`** `[tfidf → clf]` empaquetado en **`local/models/intent_tfidf_svc.joblib`** junto con metadatos (`intents`, `kind: tfidf_linearsvc_v1`, número de ejemplos). Ese archivo está en **`.gitignore`**; cada entrenamiento lo **sobrescribe**.

**Reentrenamiento:** cada vez que ejecutas el script se hace `fit` **completo** sobre todo el conjunto de entrenamiento (no es aprendizaje incremental en caliente).

### Datos de entrada

Se concatenan, en este orden, las filas válidas (`text` + `intent`) de:

- `local/datasets/intents/train_v1.jsonl` (base)
- `local/datasets/intents/train_augment.jsonl` (opcional)
- `local/datasets/intents/train_supplement.jsonl` (opcional)
- `local/datasets/intents/chat_errors.jsonl` (solo líneas con `intent` etiquetado)

Intenciones válidas: `CHECK_BALANCE`, `PAY_CREDIT_CARD`, `TRANSFER_OWN_ACCOUNTS`, `TRANSFER_THIRD_PARTY`, `AMBIGUOUS`, `OUT_OF_SCOPE`.

### Comando y métricas

El comando de arriba genera o actualiza **`local/models/intent_tfidf_svc.joblib`**. Con **`--eval`**, calcula predicciones sobre **`local/datasets/intents/eval_v1.jsonl`** e imprime accuracy y `classification_report` en consola.

Ciclo de aprendizaje (añadir ejemplos → entrenar → validar en eval y chat → errores en **`chat_errors.jsonl`** → commit y repetir): [datasets/intents/README.md](datasets/intents/README.md). Ese JSONL **se versiona** en Git para que el equipo comparta correcciones.

**Importante:** el entrenamiento **no modifica** `eval_v1.jsonl` ni `train_v1.jsonl`. El eval es un **benchmark fijo** que solo lees para medir; si quieres nuevos casos de regresión, hay que añadirlos a mano al JSONL (véase [datasets/intents/README.md](datasets/intents/README.md)).

### Probar una frase

```bash
python3 local/scripts/predict_intent.py "quiero pagar la tarjeta oro"
```

Validar contra el benchmark `eval_v1.jsonl` (simulador: caso por caso, fallo → exit code 1):

```bash
python3 local/scripts/simulate_intent_validation.py
```

Guardar fallos del benchmark en `local/datasets/intents/chat_errors.jsonl` para el siguiente entrenamiento:

```bash
python3 local/scripts/simulate_intent_validation.py --append-errors
```

Modo interactivo con corrección manual al estilo chat:

```bash
python3 local/scripts/simulate_intent_validation.py --interactive --record-chat
```

Validar "conversación local" (sin mobile, sin online) con benchmark multi-turn
de intención + siguiente acción esperada:

```bash
python3 local/scripts/simulate_dialogue_validation.py
```

Opcional: añadir fallos de intención a `chat_errors.jsonl` para reentrenar:

```bash
python3 local/scripts/simulate_dialogue_validation.py --append-errors
```

Dataset de evaluación conversacional: `local/datasets/intents/dialogue_eval_v1.jsonl`.
Propuesta de datos para entrenar conversación (curada y editable):
`local/datasets/intents/train_dialogue_supplement_v1.jsonl`.

Si quieres usar esta propuesta en entrenamiento real, copia sus filas a
`train_supplement.jsonl` (o al `train_v1.jsonl` canónico) y reentrena.

### Chat interactivo local (sin online, sin mobile)

Conversación libre contra el modelo `.joblib` ya entrenado. Calcula intención +
confianza y aplica la política local de siguiente acción (`SHOW_BALANCE`,
`SHOW_PAY_CARD_OPTIONS`, `SHOW_TRANSFER_GUIDE`, `SHOW_SUPPORT`, `ASK_CLARIFICATION`):

```bash
python3 local/scripts/simulate_local_chat.py
python3 local/scripts/simulate_local_chat.py --verbose
python3 local/scripts/simulate_local_chat.py --save-transcript local/reports/chat-session.jsonl
```

- `--verbose` imprime `[intent confidence action]` por turno.
- `--save-transcript` guarda cada turno en JSONL.
- `--clarify-threshold` ajusta el umbral de aclaración (default `0.35`).
- Enter vacío termina la sesión. Requiere el modelo entrenado.

### Reproducir escenarios del benchmark conversacional

Recorre `dialogue_eval_v1.jsonl` mostrando, por turno, la respuesta del bot y la
comparación con la intención y la acción esperadas (no modifica datos):

```bash
python3 local/scripts/replay_dialogue_scenarios.py
python3 local/scripts/replay_dialogue_scenarios.py --scenario S002
```

- `--scenario` filtra por `scenarioId`.
- `--clarify-threshold` ajusta el umbral de aclaración.

### Evaluar con Anthropic (few-shot, sin pesos en disco)

1. Exporta la API key: `export ANTHROPIC_API_KEY="..."` (opcional: `ANTHROPIC_MODEL`).
2. Desde la **raíz del repo**:

```bash
python3 local/scripts/intent_eval.py --provider anthropic --version v1
```

Salida: `local/reports/run-v1.json` y fila en `local/reports/metrics-history.csv`. Umbrales: `local/config/thresholds.json`; mock de contexto: `local/mocks/mock-user-context.json`.

**Bucle que añade ejemplos fallidos a `train_augment.jsonl` y reintenta** (mismo layout solo en `local/`):

```bash
python3 local/scripts/learn_until_pass.py --provider anthropic --session learn-1
```

### Fusionar en `train_v1.jsonl` y entrenar sklearn (automatizado)

`train_augment.jsonl` y `chat_errors.jsonl` (líneas con `intent`) ya los mezcla el entrenador **sin** copiarlos al `train_v1`; si quieres **versionar todo en `train_v1.jsonl`** y dejar augment vacío:

```bash
python3 local/scripts/finalize_intent_training.py --dry-run
python3 local/scripts/finalize_intent_training.py
```

- Hace **backup** de `train_v1.jsonl` en `local/datasets/intents/.backups/` (timestamp).
- **Sobrescribe** `train_v1.jsonl` con la unión deduplicada (mismo texto: gana corrección de `chat_errors` sobre augment sobre base).
- Ejecuta **`train_intent_classifier.py --eval`** (genera `local/models/intent_tfidf_svc.joblib`).

Opciones útiles:

- `--with-llm` — antes fusiona, ejecuta `learn_until_pass.py` (necesitas `ANTHROPIC_API_KEY` u OpenAI).
- `--clear-augment` — vacía `train_augment.jsonl` tras escribir `train_v1`.
- `--no-train` — solo fusiona y backup, sin sklearn.

Atajo: `./local/scripts/finalize_intent_training.sh` (mismos argumentos).

## Estructura esperada (después de usar)

```
local/
  README.md
  requirements.txt           # núcleo (sklearn, numpy); sirve en Python 3.14
  requirements-embeddings.txt # sentence-transformers + torch; usar Python 3.11/3.12
  .gitignore
  env.sh.example
  llevar-a-otra-laptop.md   # checklist para otra máquina
  config/
    intent_heuristic.json   # reglas JVM / coherencia con router backend
    thresholds.json         # gates para intent_eval / learn_until_pass
  datasets/intents/
    README.md
    train_v1.jsonl
    train_augment.jsonl        # generado por learn_until_pass.py (opcional)
    train_supplement.jsonl     # frases extra a mano (no versionado por defecto)
    train_supplement.jsonl.example
    train_dialogue_supplement_v1.jsonl # propuesta para conversación
    eval_v1.jsonl              # benchmark fijo de intención (copia canónica)
    dialogue_eval_v1.jsonl     # benchmark multi-turn (intención + next action)
    chat_errors.jsonl
    .backups/                  # backups de train_v1 al fusionar (ignorado)
  mocks/
    mock-user-context.json
  reports/                  # run-*.json, metrics-history.csv, learning-progress.jsonl
  scripts/
    train_intent_classifier.py
    predict_intent.py
    simulate_intent_validation.py
    simulate_dialogue_validation.py
    simulate_local_chat.py        # chat interactivo local (sin online/mobile)
    replay_dialogue_scenarios.py  # reproduce dialogue_eval_v1.jsonl y muestra respuestas
    intent_eval.py          # Anthropic / OpenAI / rule
    learn_until_pass.py
    finalize_intent_training.py
    finalize_intent_training.sh
    sklearn_refine_loop.sh
  venv/
  .cache/
  models/                    # artefactos .joblib (ignorados)
```

## Relación con el repo

- **Enrutamiento hacia el API (backend Ktor):** existe `POST /v1/chat/route`. El respaldo heurístico lee **`local/config/intent_heuristic.json`**. Con `REPO_ROOT` y el modelo `local/models/intent_tfidf_svc.joblib`, el servidor intenta `local/scripts/predict_intent.py`; si no, aplica solo ese JSON. Variables: `INTENT_ROUTER_MODE` (`auto` \| `python` \| `heuristic`), `PYTHON_BIN`, opcional `INTENT_HEURISTIC_CONFIG` (ruta absoluta al JSON). Si no encuentra el JSON, el proceso falla al arrancar: define `REPO_ROOT` o lanza Gradle desde `backend/` o la raíz del repo.
- El **benchmark** (`eval_v1.jsonl`) canónico está en **`local/datasets/intents/`**. El **entrenamiento** (`train_v1.jsonl`, `train_augment.jsonl`, etc.) está en la misma carpeta.
- **`local/datasets/intents/chat_errors.jsonl`** recoge errores del clasificador para reentrenar y **debe subirse** con Git cuando el equipo acuerde compartir correcciones (véase [datasets/intents/README.md](datasets/intents/README.md)).
- **`train_supplement.jsonl`** es opcional y por defecto puede quedar fuera de Git (`.gitignore`).
- **No** subas `venv/` ni `.cache/` ni `models/` grandes al repositorio (ya están en `local/.gitignore`).

## Desactivar el entorno

```bash
deactivate
```
