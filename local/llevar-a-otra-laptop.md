# Llevar el entorno local a otra laptop

Checklist para reproducir el **clasificador de intenciones** (TF-IDF + SVM) y validar que “aprendió”, sin depender de esta máquina.

## Qué necesitas en la otra laptop

- **Git** (para clonar o traer el repo) o una copia del proyecto.
- **Python 3.10 o superior** (3.14 sirve para el núcleo `requirements.txt`).
- Conexión a internet la **primera vez** (`pip install`).

No hace falta PyTorch ni `requirements-embeddings.txt` para este flujo.

Los comandos con `python3 local/scripts/...` deben lanzarse desde la **raíz del clon** (`ia/`). Si tu terminal está en `local/`, usa `python3 scripts/...` (sin `local/`).

## Pasos (desde cero)

### 1. Obtener el código

```bash
git clone <url-de-tu-repo> ia
cd ia
```

(Si copias la carpeta con USB, entra en la raíz del repo donde están `local/`.)

### 2. Entorno virtual solo bajo `local/`

En macOS/Linux:

```bash
python3 -m venv local/venv
source local/venv/bin/activate
python -m pip install --upgrade pip
pip install -r local/requirements.txt
```

En Windows (PowerShell):

```powershell
python -m venv local\venv
.\local\venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r local/requirements.txt
```

Comprueba:

```bash
python -c "import sklearn, numpy; print('OK')"
```

### 3. Datos en `local/datasets/intents/` (y ciclo de aprendizaje)

El **dataset base** (`train_v1.jsonl`) y el **augment** (`train_augment.jsonl`) están en **`local/datasets/intents/`**.  
En esa misma carpeta añades lo demás para seguir entrenando:

- **`chat_errors.jsonl`** — **Va en Git**: correcciones cuando el modelo falla (benchmark o chat). Tras validar, haz `commit` para que en otra laptop existan las mismas frases de refuerzo.
- **`train_supplement.jsonl`** — Opcional (plantilla: `train_supplement.jsonl.example`); por defecto está en `.gitignore` si no quieres subir frases propias; puedes forzar el commit si lo deseas.

Flujo detallado: [datasets/intents/README.md](datasets/intents/README.md).

### 4. Entrenar (aprender) con todo lo anterior

```bash
python3 local/scripts/train_intent_classifier.py --eval
```

Se usan: `local/datasets/intents/train_v1.jsonl`, `train_augment.jsonl` si existe, más `train_supplement.jsonl` y líneas de `chat_errors.jsonl` que tengan **`intent`** rellenado.

Genera:

- `local/models/intent_tfidf_svc.joblib` (ignorado por Git; en otra laptop lo regeneras o copias el archivo).

### 5. Simular, validar y guardar errores para el siguiente entrenamiento

Benchmark fijo (`eval_v1.jsonl`):

```bash
python3 local/scripts/simulate_intent_validation.py
```

Si hubo fallos y quieres **volcarlos** a `local/datasets/intents/chat_errors.jsonl` (con la intención correcta ya puesta) para reentrenar después:

```bash
python3 local/scripts/simulate_intent_validation.py --append-errors
```

Modo tipo chat: escribes frases y, si la predicción es mala, indicas la intención correcta; eso se guarda en el mismo `chat_errors.jsonl`:

```bash
python3 local/scripts/simulate_intent_validation.py --interactive --record-chat
```

Una sola frase (sin guardar):

```bash
python3 local/scripts/simulate_intent_validation.py --message "cuanto tengo en el banco"
```

Detalle de formatos y flujo: [datasets/intents/README.md](datasets/intents/README.md).

### 6. (Opcional) Probar una sola predicción

```bash
python3 local/scripts/predict_intent.py "quiero pagar la tarjeta"
```

## Si no quieres volver a entrenar

Puedes copiar solo el artefacto entre laptops (misma versión de dependencias recomendada: mismo `local/requirements.txt` y mismo Python mayor si es posible):

- Origen: `local/models/intent_tfidf_svc.joblib`
- Destino: la misma ruta en el otro clon.

Opcional: si usas `train_supplement.jsonl` solo en tu máquina, cópialo aparte. `chat_errors.jsonl` debería llegar con el **clone/pull** del repo si el equipo lo sube.

Luego ejecuta el paso 5 para validar.

## Qué no suele viajar en Git

| Ruta | Motivo |
|------|--------|
| `local/venv/` | Entorno virtual; se recrea en cada máquina |
| `local/models/*.joblib` | Modelo entrenado; ignorado por git |
| `local/datasets/intents/train_supplement.jsonl` | Opcional ignorado; solo si no quieres subirlo |
| `local/.cache/` | Caché opcional (p. ej. Hugging Face) |

## Más documentación

- Datos locales y ciclo error → reentreno: `local/datasets/intents/README.md`
- Detalle del entorno Python: `local/README.md`
