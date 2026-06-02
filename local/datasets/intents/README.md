# Ciclo de aprendizaje del clasificador (`local/datasets/intents/`)

Objetivo: **seguir mejorando** el router de intenciones (TF-IDF local) de forma repetible: añadir ejemplos → entrenar → validar en benchmark y en “chat” → **registrar errores en JSON** → volver a entrenar y comprobar que aprendió.

`chat_errors.jsonl` **sí se versiona en Git**: así el equipo comparte correcciones y el historial de frases mal clasificadas no se pierde al cambiar de laptop.

**Rutas de los comandos:** Ejecuta desde la **raíz del repo** (`ia/`). Si estás dentro de `local/`, quita el prefijo `local/` en los ejemplos (p. ej. `python3 scripts/train_intent_classifier.py --eval`).

## Ciclo recomendado (resumen)

1. **Añadir material de entrenamiento** — Frases nuevas en `train_supplement.jsonl` (opcional, ver abajo) y/o filas en `chat_errors.jsonl` (tras un fallo, con `intent` correcto).
2. **Entrenar** — `python3 local/scripts/train_intent_classifier.py --eval`
3. **Validar en benchmark** — `python3 local/scripts/simulate_intent_validation.py` (debe dar accuracy alta en `eval_v1.jsonl`).
4. **Validar en chat** — `python3 local/scripts/simulate_intent_validation.py --interactive --record-chat` (pruebas libres; si el modelo falla, indicas la intención correcta y se añade línea al JSON).
5. **Volcar fallos del benchmark al JSON** (si los hubo) — `python3 local/scripts/simulate_intent_validation.py --append-errors`
6. **Subir cambios** — `git add` / `commit` de `chat_errors.jsonl` (y de `train_supplement.jsonl` si lo pasas a versionado) para que otros tiren y reentrenen.
7. **Repetir** desde el paso 2 hasta que el chat y el eval queden estables.

```text
  [ datos: train_v1 + augment + supplement + chat_errors ]
                        ↓
                  train_intent_classifier.py
                        ↓
                  intent_tfidf_svc.joblib (local, no en Git)
                        ↓
     simulate_intent_validation.py  (eval + / o chat interactivo)
                        ↓
            ¿error? →  línea en chat_errors.jsonl  →  commit →  reentrenar
```

## Archivos en esta carpeta

| Archivo | ¿En Git? | Rol |
|---------|----------|-----|
| `train_v1.jsonl` | **Sí** | Dataset base manual. |
| `train_augment.jsonl` | Opcional | Generado por `learn_until_pass.py`; few-shot y similitud en `simulate_user_chat.py`. |
| `chat_errors.jsonl` | **Sí** | Frases donde el modelo falló y la **intención correcta**; el entrenador usa líneas con **`intent`** rellenado. |
| `train_supplement.jsonl` | No por defecto (opcional en `.gitignore`) | Frases extra que añades a mano; mismo formato que `train_v1`. Puedes versionarla si quieres (`git add -f`). |
| `train_supplement.jsonl.example` | Sí | Plantilla de formato |
| `train_dialogue_supplement_v1.jsonl` | Sí | Propuesta inicial de frases para robustecer conversación local (sin online). |
| `dialogue_eval_v1.jsonl` | Sí | Benchmark multi-turn para validar intención + siguiente acción conversacional. |

Intenciones válidas: `CHECK_BALANCE`, `PAY_CREDIT_CARD`, `TRANSFER_OWN_ACCOUNTS`, `TRANSFER_THIRD_PARTY`, `AMBIGUOUS`, `OUT_OF_SCOPE`.

El benchmark fijo está en **`local/datasets/intents/eval_v1.jsonl`**.

---

## Refinar `train_v1.jsonl` y `eval_v1.jsonl` (para que sklearn rinda bien)

### Roles

| Archivo | Función |
|---------|---------|
| **`train_v1.jsonl`** | Muchos ejemplos **variados** por intención (formas distintas de decir lo mismo). Es la “comida” del modelo. |
| **`eval_v1.jsonl`** | **Benchmark pequeño y estable**: casos que quieres que **sigan pasando** en cada release (regresión). Formato: `id`, `text`, `expectedIntent`, `expectedEntities`, `sensitive`. |

**Consejo:** si el eval solo copia frases idénticas del train, la métrica puede inflarse; para medir generalización, usa en eval **paráfrasis** que también estén en el train como otras variantes, o frases nuevas. Para **regresión** (como E020–E026) está bien fijar frases concretas que antes fallaban.

### ¿Anthropic u otro LLM?

**No entrenan el `.joblib`.** Sirven como **ayuda**:

1. **Generar paráfrases** por intención (“dame 20 formas coloquiales de pedir saldo en Perú”) → revisas tú, quitas rarezas, pegas líneas JSON en `train_supplement.jsonl` o directamente en `train_v1.jsonl`.
2. **Comparar** con el benchmark sin sklearn: `python3 local/scripts/intent_eval.py --provider anthropic --version v1` (few-shot en la nube; ver `local/README.md`).
3. **Bucle de fallos** hacia `train_augment.jsonl`: `learn_until_pass.py` (también en `local/README.md`).

Flujo útil: **LLM genera borrador → humano corrige etiquetas → entrenas sklearn**.

### Sin API: iteración manual o asistente en el IDE

Puedes editar los JSONL a mano o pedir en el chat del IDE (por intención o tema: “OUT_OF_SCOPE bancario”, “disparadores de pago TC”) que te propongan **líneas nuevas en formato JSONL**; luego tú validas negocio y las añades.

### Cerrar el ciclo con sklearn

Tras cualquier cambio serio en train o eval:

```bash
python3 local/scripts/train_intent_classifier.py --eval
```

Comprueba accuracy en `eval_v1.jsonl` y, si baja, revisa conflictos entre clases o añade ejemplos en la intención que falla.

### Automatizar reintentos (solo sklearn + `chat_errors`)

**No** reescribe solo `train_v1.jsonl` / `eval_v1.jsonl` en bucle: eso sigue siendo decisión humana o flujo aparte.

Lo que sí puedes hacer en bucle:

1. **`local/scripts/sklearn_refine_loop.sh`** (desde la raíz del repo): entrena → valida con `simulate_intent_validation.py` → si hay fallos, los vuelca a **`chat_errors.jsonl`** (etiqueta correcta del eval) → vuelve a entrenar. Repite hasta pasar el benchmark o agotar iteraciones. Útil para que el modelo **memorice** correcciones del eval; si no converge, falta variedad en `train_v1` o hay solapamiento entre intenciones.

2. **Con API (Anthropic u OpenAI):** `local/scripts/learn_until_pass.py` — evalúa con few-shot en la nube, añade fallos a **`train_augment.jsonl`**, reintenta hasta un *promotion gate* (ver `local/README.md`). Tampoco sustituye editar a mano el `train_v1` canónico si quieres control fino.

La **heurística** (`intent_heuristic.json`) no tiene bucle automático: hay que editar reglas o generar texto y pegarlo.

---

## Cómo añadir mensajes para que siga aprendiendo

### A) Manualmente en `train_supplement.jsonl`

1. Copia la plantilla:  
   `cp local/datasets/intents/train_supplement.jsonl.example local/datasets/intents/train_supplement.jsonl`
2. Una línea JSON por frase:

   ```json
   {"text":"mi frase nueva","intent":"CHECK_BALANCE","entities":{}}
   ```

3. Entrena y valida (pasos del ciclo arriba).

### B) Tras un fallo en el benchmark (`eval_v1.jsonl`)

```bash
python3 local/scripts/simulate_intent_validation.py --append-errors
```

Se añaden a **`chat_errors.jsonl`** las clasificaciones incorrectas, con la intención correcta del benchmark. Luego **commit** del archivo y reentreno.

### C) Tras un fallo en el chat simulado

```bash
python3 local/scripts/simulate_intent_validation.py --interactive --record-chat
```

Si la predicción es mala, escribe la intención correcta cuando lo pida; la línea se guarda en **`chat_errors.jsonl`**. Si acertó, pulsa Enter en “Corrección” y no se guarda nada.

### D) Validar conversación local (sin mobile, sin online)

```bash
python3 local/scripts/simulate_dialogue_validation.py
```

Este benchmark mide dos cosas por turno:

- `expectedIntent` vs predicción.
- `expectedNextAction` vs política local (`SHOW_BALANCE`, `SHOW_PAY_CARD_OPTIONS`, `SHOW_TRANSFER_GUIDE`, `SHOW_SUPPORT`, `ASK_CLARIFICATION`).

Si quieres pasar fallos de intención a `chat_errors.jsonl` para reentrenar:

```bash
python3 local/scripts/simulate_dialogue_validation.py --append-errors
```

Para empezar a mejorar conversación, toma ejemplos desde
`train_dialogue_supplement_v1.jsonl` y llévalos a `train_supplement.jsonl`
(o al `train_v1.jsonl` canónico), luego reentrena.

---

## Entrenar de nuevo (mezcla de fuentes)

El script une, en este orden:

`train_v1.jsonl` + `train_augment.jsonl` (si existe) + `train_supplement.jsonl` (si existe) + filas útiles de **`chat_errors.jsonl`** (todo en esta carpeta salvo que indiques otra ruta).

```bash
python3 local/scripts/train_intent_classifier.py --eval
```

---

## Formato de una línea en `chat_errors.jsonl`

Ejemplo generado por el simulador:

```json
{"text":"frase del usuario","intent":"CHECK_BALANCE","predictedIntent":"AMBIGUOUS","source":"eval_fail","caseId":"E017","entities":{}}
```

- El entrenador **omite** líneas sin `intent` o con `"intent": null` (pendientes de etiquetar).
- `entities` puede ser `{}` si solo entrenas el router de intención.
