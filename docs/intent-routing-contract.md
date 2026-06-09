# Contrato de enrutamiento de intenciones

Documento canónico del flujo **texto del usuario → intención → acción sugerida → UI/API**.
Cualquier cambio en umbrales, labels o mapeos debe actualizar este archivo.

## Intenciones válidas

| Label | Descripción |
|-------|-------------|
| `CHECK_BALANCE` | Consultar saldo o cuentas |
| `PAY_CREDIT_CARD` | Pagar tarjeta de crédito |
| `TRANSFER_OWN_ACCOUNTS` | Transferencia entre cuentas propias |
| `TRANSFER_THIRD_PARTY` | Transferencia a terceros |
| `VIEW_CHAT_HISTORY` | Ver historial de conversaciones con el asistente |
| `MONTHLY_EXPENSES` | Gastos del mes agrupados por categoría |
| `AMBIGUOUS` | No se puede clasificar con confianza |
| `OUT_OF_SCOPE` | Fuera del alcance bancario del asistente |

Fuente de verdad en código:
- Backend enum: `backend/domain/.../IntentLabel.kt`
- Datasets: `local/datasets/intents/*.jsonl`
- Heurística JVM: `local/config/intent_heuristic.json`

## Flujo end-to-end

```mermaid
sequenceDiagram
    participant U as Usuario
    participant M as Mobile
    participant API as POST /v1/chat/message
    participant B as BuildChatUiUseCase
    participant C as IntentClassifier

    U->>M: mensaje chat
    M->>API: SendChatMessageUseCase
    API->>B: token + message
    B->>C: classify(text)
    B->>B: ChatUiBuilder (saldo / pago / …)
    B-->>API: uiTree JSON
    API-->>M: ChatMessageResponse
    M->>M: SduiRenderer
    M-->>U: paneles SDUI
    U->>M: Pagar ahora
    M->>API: POST /v1/credit-cards/{id}/payments
```

Legacy solo intención (eval / herramientas): `POST /v1/chat/route` → `RouteChatMessageUseCase` + `BackendRouteResolver`.

## Capas y responsabilidades

| Capa | Responsabilidad | No debe |
|------|-----------------|---------|
| **Woz** (`api/intent/` + Ollama) | Predecir `IntentLabel` + confidence | Ejecutar pagos ni consultar saldo real |
| **BuildChatUiUseCase** + `application/sdui/*` | Clasificar + armar `uiTree` con datos reales (Mongo) | Ejecutar pagos |
| **BackendRouteResolver** | Mapear intención → hints (ruta legacy `/chat/route`) | — |
| **SduiRenderer** (mobile) | Pintar catálogo Compose según `type` + props | Clasificar ni armar layout de negocio |
| **ChatViewModel** | Enviar mensaje, render SDUI, `PayCreditCardUseCase` al pulsar pagar | Inventar balances |

## Mapeo intención → API

| Intención | Endpoints sugeridos | Implementado |
|-----------|--------------------|--------------|
| `CHECK_BALANCE` | `GET /v1/me/balance` | Sí |
| `PAY_CREDIT_CARD` | `GET /v1/credit-cards/with-debt`, `POST /v1/credit-cards/{cardId}/payments` | Sí |
| `TRANSFER_OWN_ACCOUNTS` | `POST /v1/transfers/own` | **No** (`implemented = false`) |
| `TRANSFER_THIRD_PARTY` | `POST /v1/transfers/third-party` | **No** |
| `VIEW_CHAT_HISTORY` | `GET /v1/me/chat/history` | Sí |
| `MONTHLY_EXPENSES` | `GET /v1/me/expenses/by-category?yearMonth=YYYY-MM` | Sí |
| `AMBIGUOUS`, `OUT_OF_SCOPE` | (vacío) | — |

**Regla:** si `implemented = false`, la UI debe informar que la operación no está disponible;
no simular éxito de transferencia.

## Umbrales de confianza (estado actual)

> **Nota:** existen diferencias entre capas. Al unificar, actualizar esta tabla y los tests.

| Capa | Archivo | Umbral / comportamiento |
|------|---------|-------------------------|
| Mobile UI policy | `ChatPresentationPolicy.kt` (backend SDUI) | Clarificación si `confidence < 0.55` o `AMBIGUOUS` |
| OUT_OF_SCOPE recheck | `ChatPresentationPolicy.kt` | Re-check si `confidence < 0.8` y texto parece bancario |
| Local simulador | `simulate_woz_chat.py` | `ASK_CLARIFICATION` si `confidence < 0.55` |
| Backend heurístico | `HeuristicIntentClassifier.kt` | Reglas JSON + `BankingIntentKeywordResolver` si sigue `AMBIGUOUS` (p. ej. "pagando mi tc", saludo + operación) |
| Backend auto — fast path | `HeuristicFastPathPolicy.kt` | Heurística aceptada sin LLM si `intent != AMBIGUOUS`, `!clarificationNeeded` y `confidence >= 0.85` (`INTENT_HEURISTIC_FAST_PATH_MIN_CONFIDENCE`) |
| Backend auto — fallback Woz | `CascadeIntentClassifier.kt` | Tras Woz: heurística si confianza `< 0.55`, `AMBIGUOUS` o `clarificationNeeded` (`WOZ_MIN_CONFIDENCE_FOR_ACCEPT`) |
| Promotion LLM eval | `local/config/thresholds.json` | `minIntentAccuracyGlobal: 0.9`, etc. |

**Objetivo:** converger umbrales de clarificación en un solo config compartido o documentar
explícitamente por qué difieren (p. ej. mobile más conservador en UI).

## Cascade heurística + LLM (modo `auto`)

En modo `auto`, el backend usa **`CascadeIntentClassifier`**: decide la intención en tres pasos antes de armar SDUI.

```mermaid
flowchart TD
    A[Mensaje del usuario] --> B[Heurística JVM<br/>intent_heuristic.json]
    B --> C{¿Match satisfactorio?<br/>intent claro + conf ≥ 0.85}
    C -->|Sí| D[Usar intent heurístico<br/>source: heuristic<br/>Sin llamar a Ollama]
    C -->|No| E[Woz — LLM local Ollama<br/>con historial de sesión]
    E --> F{¿Woz OK?<br/>conf ≥ 0.55, no AMBIGUOUS}
    F -->|Sí| G[Usar intent Woz<br/>source: woz]
    F -->|No / error HTTP| H[Heurística otra vez<br/>source: heuristic]
    D --> I[BuildChatUiUseCase<br/>builder SDUI + datos MongoDB]
    G --> I
    H --> I
```

### Qué es un match heurístico “satisfactorio” (fast path)

La heurística **no genera la respuesta visual**; solo etiqueta la intención. Se considera suficiente para **omitir Ollama** cuando:

1. `intent != AMBIGUOUS`
2. `clarificationNeeded == false` (regla interna heurística: confianza ≥ 0.65 y intent claro)
3. `confidence >= INTENT_HEURISTIC_FAST_PATH_MIN_CONFIDENCE` (default **0.85**)

Ejemplo: *"ver mi saldo"* → regla `CHECK_BALANCE` con confianza 0.92 → respuesta SDUI de saldo **sin** invocar el LLM.

Ejemplo: *"paga 50"* sin contexto → heurística `AMBIGUOUS` → **sí** invoca Woz (necesita historial).

### Cuándo entra Woz

- La heurística no matcheó con confianza suficiente.
- Mensajes cortos o follow-ups donde las reglas JSON no alcanzan.

Woz recibe el mensaje + hasta **6 turnos** previos (JSON compacto con intent/entities del asistente).

### Cuándo vuelve la heurística (tras Woz)

- Ollama caído, timeout o JSON inválido.
- Woz devuelve `AMBIGUOUS`, `clarificationNeeded` o `confidence < WOZ_MIN_CONFIDENCE_FOR_ACCEPT` (default 0.55).

### Mejorar cobertura heurística

Ampliar `local/config/intent_heuristic.json` aumenta cuántos mensajes resuelve el **fast path** (más rápido, menos dependencia de Ollama). Ver `local/datasets/intents/README.md`.

### Excepciones que omiten clasificadores

- **Quick reply** del `GreetingCard` (`selectedIntent` permitido): intención fijada por servidor (`source: quick_reply`), sin heurística ni Woz.
- Modos forzados: `INTENT_ROUTER_MODE=woz` (solo LLM) o `heuristic` (solo JSON).

Tras clasificar, **`BuildChatUiUseCase`** + builders SDUI consultan MongoDB y arman `uiTree`. Ni heurística ni LLM ejecutan pagos ni inventan saldos.

## Next actions (simulador local / dialogue eval)

Usados en `dialogue_eval_v1.jsonl` y scripts `simulate_dialogue_validation.py`:

| Action | Cuándo |
|--------|--------|
| `SHOW_BALANCE` | `CHECK_BALANCE` con confianza suficiente |
| `SHOW_PAY_CARD_OPTIONS` | `PAY_CREDIT_CARD` |
| `SHOW_TRANSFER_GUIDE` | `TRANSFER_*` |
| `SHOW_SUPPORT` | `OUT_OF_SCOPE` |
| `ASK_CLARIFICATION` | `AMBIGUOUS` o baja confianza |

## Benchmarks obligatorios

Desde la **raíz del repo**:

```bash
python3 local/scripts/simulate_intent_validation.py
python3 local/scripts/simulate_dialogue_validation.py
```

Datasets:
- `local/datasets/intents/eval_v1.jsonl` — clasificación por frase
- `local/datasets/intents/dialogue_eval_v1.jsonl` — multi-turn

## Modos del clasificador (backend) — Woz

Variable `INTENT_ROUTER_MODE`:

| Modo | Comportamiento |
|------|----------------|
| `auto` (default) | **Cascade**: heurística (fast path) → Woz → heurística si Woz falla o duda. Ver sección [Cascade heurística + LLM](#cascade-heurística--llm-modo-auto). |
| `woz` | Solo Woz (`WozIntentClassifier`) |
| `heuristic` | Solo `local/config/intent_heuristic.json` |

Variables:

| Variable | Default | Uso |
|----------|---------|-----|
| `WOZ_OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Base Ollama |
| `WOZ_MODEL` | `qwen2.5:7b-instruct` | Modelo |
| `WOZ_TIMEOUT_SECONDS` | `60` | Timeout HTTP |
| `WOZ_MIN_CONFIDENCE_FOR_ACCEPT` | `0.55` | Tras Woz en `auto`: si baja → heurística |
| `INTENT_HEURISTIC_FAST_PATH_MIN_CONFIDENCE` | `0.85` | En `auto`: heurística aceptada sin LLM |

Requisito para Woz: **Ollama en marcha** con el modelo indicado (ver `local/README.md`). En `auto`, frases cubiertas por el JSON heurístico **no requieren** Ollama.

Metadata SDUI: `routerSource` = `woz` \| `heuristic` \| `quick_reply`.
