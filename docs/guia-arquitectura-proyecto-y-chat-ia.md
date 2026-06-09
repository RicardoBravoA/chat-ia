# Guía de arquitectura del proyecto y chat de IA

Documento de referencia sobre cómo funciona el repositorio **ia/**: app bancaria KMP, backend Ktor y asistente de chat con SDUI + Woz (Ollama).

## Visión general del proyecto

Es una **app bancaria real** para Android/iOS con tres piezas que corren de forma independiente pero se integran en el chat:

| Pieza | Stack | Rol |
|-------|-------|-----|
| **`mobile/`** | Kotlin Multiplatform + Compose | UI, sesión, render SDUI, pagos con confirmación |
| **`backend/`** | Kotlin JVM 21 + Ktor + MongoDB | API, reglas de negocio, clasificación IA, armado de UI |
| **`local/`** | Python + Ollama | LLM local (**Woz**) para clasificar intenciones y benchmarks |

Prioridades transversales: **seguridad**, **auditabilidad** (correlation IDs, historial de sesión) y **no inventar datos bancarios**.

---

## Arquitectura global

```mermaid
flowchart TB
    subgraph Mobile["mobile/ — KMP Compose"]
        UI[ChatScreen / Home / Login]
        VM[ViewModels + StateFlow]
        UC_M[Use Cases]
        SDUI[SduiRenderer]
        UI --> VM --> UC_M
        VM --> SDUI
    end

    subgraph Backend["backend/ — Ktor hexagonal"]
        API[api/ — rutas HTTP + WS]
        APP[application/ — use cases + SDUI builders]
        DOM[domain/ — modelos + ports]
        INFRA[infrastructure-mongo/]
        API --> APP --> DOM
        INFRA --> DOM
    end

    subgraph Local["local/ — Woz"]
        OLLAMA[Ollama qwen2.5:7b-instruct]
        SCRIPTS[Scripts Python de eval]
        OLLAMA --- SCRIPTS
    end

    subgraph Data["Persistencia"]
        MONGO[(MongoDB banking)]
    end

    UC_M -->|REST + WebSocket| API
    APP -->|WozIntentClassifier| OLLAMA
    INFRA --> MONGO
```

---

## Backend (hexagonal)

Cuatro módulos Gradle, de adentro hacia afuera:

```mermaid
flowchart LR
    subgraph domain["domain"]
        Models[IntentLabel, UiNode, CreditCard…]
        Ports[SessionRepository, IntentClassifierPort, ChatSessionRepository…]
    end

    subgraph application["application"]
        UC[BuildChatUiUseCase, PayCreditCardUseCase…]
        SDUI_B[BalanceUiBuilder, PayCreditCardUiBuilder…]
        Policy[ChatPresentationPolicy]
    end

    subgraph infra["infrastructure-mongo"]
        MongoRepos[MongoChatSessionRepository, MongoExpenseRepository…]
    end

    subgraph api["api"]
        Routes[BankingV1Routes.kt]
        Woz[WozIntentClassifier + HeuristicIntentClassifier]
        Root[BankingCompositionRoot — wiring]
    end

    api --> application --> domain
    infra --> domain
    Woz -.->|implementa| Ports
    MongoRepos -.->|implementa| Ports
```

**Regla clave:** la lógica de negocio vive en `application/`; las rutas Ktor solo validan auth y delegan.

---

## Mobile (Clean Architecture en un solo módulo)

```mermaid
flowchart TB
    subgraph presentation
        Screens[ChatScreen, HomeScreen]
        ChatVM[ChatViewModel]
        Renderer[SduiRenderer]
    end

    subgraph domain
        UseCases[SendChatMessageUseCase, PayCreditCardUseCase…]
        RepoIf[HomeRepository interface]
        SduiModels[UiNode, ChatUiResponse]
    end

    subgraph data
        Remote[RemoteHomeRepository]
        Ktor[Ktor HTTP + WebSocket]
    end

    Screens --> ChatVM
    ChatVM --> UseCases
    UseCases --> RepoIf
    Remote -.-> RepoIf
    Remote --> Ktor
    ChatVM --> Renderer
```

El wiring manual está en `BankingApp.kt`: repositorios → use cases → ViewModels.

---

## Cómo funciona el chat de IA (flujo completo)

El chat **no es un LLM que redacta respuestas libres**. Es un **orquestador**:

1. **Woz** (LLM local) solo **clasifica la intención** del usuario.
2. El backend **consulta datos reales** en MongoDB (saldo, tarjetas, gastos…).
3. El backend **arma un árbol JSON** (`uiTree`) con componentes predefinidos (SDUI).
4. Mobile **solo renderiza** ese árbol; los pagos van por API transaccional aparte.

### Diagrama de secuencia (flujo principal)

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant CS as ChatScreen
    participant VM as ChatViewModel
    participant UC as SendChatMessageUseCase
    participant WS as WS /v1/chat/ws
    participant BUI as BuildChatUiUseCase
    participant SESS as ChatSessionRepository
    participant RC as RouteChatMessageUseCase
    participant WOZ as Woz (Ollama)
    participant HEU as Heurística JVM
    participant POL as ChatPresentationPolicy
    participant BLD as ChatUiBuilderFactory
    participant DB as MongoDB
    participant REN as SduiRenderer
    participant PAY as PayCreditCardUseCase

    U->>CS: "paga mi tc"
    CS->>VM: sendMessage()
    VM->>UC: token + message + sessionId
    UC->>WS: WebSocket + Bearer token
    WS->>BUI: execute()

    BUI->>SESS: listRecentTurns (últimos 6)
    SESS->>DB: chat_sessions
    DB-->>SESS: historial

    BUI->>RC: classify(message, history)
    RC->>WOZ: classify (modo auto)
    alt confianza baja o AMBIGUOUS
        WOZ-->>RC: intent + confidence
        RC->>HEU: fallback heurístico
        HEU-->>RC: intent refinado
    else confianza OK
        WOZ-->>RC: PAY_CREDIT_CARD, 0.93
    end
    RC-->>BUI: IntentClassification

    BUI->>POL: shouldAskClarification? shouldShowGreeting?
    POL-->>BUI: no → builder de pago

    BUI->>BLD: forIntent(PAY_CREDIT_CARD)
    BLD->>DB: tarjetas con deuda (use case)
    DB-->>BLD: datos reales
    BLD-->>BUI: uiTree (Column + AssistantText + PayCardPanel×N)

    BUI->>SESS: appendTurn (auditoría)
    BUI-->>WS: UiResponse
    WS-->>UC: ChatMessageResponse JSON
    UC-->>VM: ChatUiResponse
    VM->>REN: render uiTree
    REN-->>U: paneles de pago con deuda real

    U->>CS: pulsa "Pagar mínimo"
    CS->>VM: payCreditCard()
    Note over VM,PAY: Biometría + Idempotency-Key
    VM->>PAY: POST /v1/credit-cards/{id}/payments
    PAY-->>VM: comprobante
    VM-->>U: recibo local post-pago
```

### Diagrama de decisiones en el backend

```mermaid
flowchart TD
    START[Mensaje usuario + sessionId] --> AUTH{Token válido?}
    AUTH -->|No| ERR401[401 Unauthorized]
    AUTH -->|Sí| QR{Quick reply<br/>selectedIntent?}
    QR -->|Sí| SKIP[Clasificación directa<br/>confidence = 1.0]
    QR -->|No| HIST[Cargar últimos 6 turnos de Mongo]
    HIST --> CLASS[RouteChatMessageUseCase]

    CLASS --> MODE{INTENT_ROUTER_MODE}
    MODE -->|woz| WOZ_ONLY[WozIntentClassifier]
    MODE -->|heuristic| HEU_ONLY[HeuristicIntentClassifier]
    MODE -->|auto| CASCADE[Heurística fast path<br/>→ Woz si no<br/>→ heurística si Woz falla]

    SKIP --> POL
    WOZ_ONLY --> POL
    HEU_ONLY --> POL
    CASCADE --> POL

    POL{ChatPresentationPolicy}
    POL -->|Saludo sin operación| GREET[GreetingUiBuilder]
    POL -->|Baja confianza / AMBIGUOUS| CLAR[ClarificationUiBuilder]
    POL -->|OUT_OF_SCOPE| SUP[SupportUiBuilder]
    POL -->|Intención clara| FACT[ChatUiBuilderFactory]

    FACT --> BAL[CHECK_BALANCE → BalanceUiBuilder]
    FACT --> PAY[PAY_CREDIT_CARD → PayCreditCardUiBuilder]
    FACT --> HIST2[VIEW_CHAT_HISTORY → ChatHistoryUiBuilder]
    FACT --> EXP[MONTHLY_EXPENSES → MonthlyExpensesUiBuilder]
    FACT --> XFER[TRANSFER_* → NotImplementedTransferUiBuilder]

    GREET --> TREE[uiTree JSON]
    CLAR --> TREE
    SUP --> TREE
    BAL --> TREE
    PAY --> TREE
    HIST2 --> TREE
    EXP --> TREE
    XFER --> TREE

    TREE --> SAVE[Guardar turno en chat_sessions]
    SAVE --> RESP[ChatMessageResponse<br/>correlationId + sessionId + metadata]
```

---

## Piezas importantes del chat

### 1. Woz — clasificador IA (no ejecuta dinero)

- Corre en **Ollama** local (`qwen2.5:7b-instruct`).
- Recibe el mensaje + historial (últimos 6 turnos).
- Devuelve JSON: `{ intent, confidence, entities, clarification_needed, reason }`.
- Modo `auto` (default): **cascade** — heurística JVM primero (fast path); si no hay match claro → Woz; si Woz falla o duda → heurística otra vez. Ver [`docs/intent-routing-contract.md`](intent-routing-contract.md#cascade-heurística--llm-modo-auto).

Intenciones soportadas: `CHECK_BALANCE`, `PAY_CREDIT_CARD`, `TRANSFER_*`, `VIEW_CHAT_HISTORY`, `MONTHLY_EXPENSES`, `AMBIGUOUS`, `OUT_OF_SCOPE`.

### 2. SDUI — Server-Driven UI

El backend devuelve un árbol como este:

```json
{
  "uiTree": {
    "type": "Column",
    "children": [
      { "type": "AssistantText", "props": { "text": "..." } },
      { "type": "PayCardPanel", "props": { "cardId": "...", "debt": "4200.0" } }
    ]
  },
  "metadata": { "intent": "PAY_CREDIT_CARD", "confidence": 0.93, "routerSource": "woz" }
}
```

Mobile mapea cada `type` a un Composable fijo en `SduiRenderer` (`BalanceCard`, `PayCardPanel`, `GreetingCard`, etc.).

**Copy grounded:** el texto lo genera `GroundedChatCopy` con datos reales de Mongo, **no el LLM**, para no inventar montos.

### 3. Sesión multi-turn

- Colección Mongo `chat_sessions`.
- Primer mensaje sin `sessionId` → backend crea sesión y devuelve el ID.
- Mobile reenvía el mismo `sessionId` en cada mensaje del hilo.
- Woz usa el historial para follow-ups como *"paga 50"* o *"la oro"*.

### 4. Transporte: WebSocket

La app mobile usa **`WS /v1/chat/ws`**: abre conexión, envía un frame JSON, recibe `chat_response` y cierra.

Legacy / eval: `POST /v1/chat/route` (solo clasificación, sin SDUI).

### 5. Seguridad en pagos

```mermaid
flowchart LR
    SDUI[PayCardPanel en uiTree] --> TAP[Usuario pulsa pagar]
    TAP --> BIO[Confirmación de Pago]
    BIO --> API[POST /v1/credit-cards/id/payments]
    API --> IDEM[Header Idempotency-Key]
    IDEM --> VAL[Validación backend:<br/>fondos, límites, deuda]
    VAL --> MONGO[(Registrar movimiento)]
```

El clasificador **nunca** ejecuta pagos. Solo sugiere qué UI mostrar.

---

## Otros flujos de la app (fuera del chat)

| Feature | Mobile | Backend |
|---------|--------|---------|
| Login | `LoginUseCase` | `POST /v1/auth/login` → token Bearer |
| Home / saldo | `GetBalanceUseCase` | `GET /v1/me/balance` |
| Movimientos / pagos | `GetPaymentsUseCase` | endpoints de tarjetas |
| Pago TC manual | `PayCreditCardUseCase` | `POST .../payments` + idempotencia |

---

## Cómo ponerlo en marcha (resumen)

1. **MongoDB** — datos de usuarios, tarjetas, sesiones de chat.
2. **Ollama + modelo** — clasificación Woz en el backend.
3. **Backend** — `cd backend && ./gradlew :api:run` → `:8080`.
4. **Mobile** — login demo `woz@bank.com` / `Demo1234!`, chat en tiempo real vía WebSocket.

Benchmarks opcionales:

```bash
python3 local/scripts/simulate_intent_validation.py
python3 local/scripts/simulate_woz_chat.py
```

---

## Documentación relacionada

| Documento | Contenido |
|-----------|-----------|
| [README.md](../README.md) | Puesta en marcha rápida |
| [AGENTS.md](../AGENTS.md) | Guía para desarrolladores y agentes |
| [server-driven-ui-blueprint.md](server-driven-ui-blueprint.md) | SDUI y catálogo de componentes |
| [intent-routing-contract.md](intent-routing-contract.md) | Intenciones, umbrales, mapeo API |
| [ai-chat-backend-blueprint.md](ai-chat-backend-blueprint.md) | Diseño objetivo del chat IA |
| [presentacion-dev-chat-ia.md](presentacion-dev-chat-ia.md) | Presentación técnica del chat |
