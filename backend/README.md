# Backend (KMP / Ktor + MongoDB)

API bancaria con arquitectura limpia/hexagonal por módulos Gradle. El dominio
posee las invariantes de negocio; la capa `api` valida contratos y delega en
casos de uso de `application`; `infrastructure-mongo` adapta la persistencia.

## Módulos

| Módulo | Rol |
|--------|-----|
| `domain` | Modelos, reglas e invariantes de negocio (sin frameworks). |
| `application` | Casos de uso que orquestan el dominio. |
| `infrastructure-mongo` | Adaptadores de persistencia (MongoDB) y bootstrap de datos demo. |
| `api` | Servidor Ktor (Netty), rutas `/v1`, auth y mapeo de DTOs. |

## Requisitos previos

- **JDK 21** (los módulos fijan `jvmToolchain(21)`; Gradle puede descargarlo).
- **MongoDB** escuchando en `27017`. Guía completa: [`docs/mongodb-setup.md`](docs/mongodb-setup.md).

Levantar Mongo rápido con Docker:

```bash
docker run -d --name banking-mongo -p 27017:27017 mongo:7
```

## Ejecutar el servidor

**Requisitos para chat con IA:** MongoDB + **Ollama** con el modelo Woz (ver abajo).

Desde `backend/`:

```bash
./gradlew :api:run
```

- Arranca en `http://localhost:8080`.
- Al iniciar ejecuta `MongoBootstrap` (índices + datos demo) sobre la base `banking`.
- Health check: `GET /health` → `{"status":"ok"}`.

### Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `MONGO_URI` | `mongodb://127.0.0.1:27017` | Conexión a MongoDB. |
| `PORT` | `8080` | Puerto HTTP del API. |

### Chat realtime (`WS /v1/chat/ws`)

Canal WebSocket para chat SDUI en tiempo real:

- Cliente envía `ChatMessageRequest` JSON (`{"message":"...","sessionId":"..."}`). Si `sessionId` es `null` u omitido, el backend crea una sesión nueva.
- Chips `QUICK_REPLY` del `GreetingCard` pueden enviar `selectedIntent` (`CHECK_BALANCE`, `PAY_CREDIT_CARD`, `MONTHLY_EXPENSES`, `VIEW_CHAT_HISTORY`, `TRANSFER_OWN_ACCOUNTS`); el backend **omite Woz** solo para esos intents servidos en el saludo.
- Backend responde `ChatWsEnvelope` con `event = "chat_response"` y `response` (`ChatMessageResponse` con `uiTree` y `sessionId`).
- Las sesiones se persisten en MongoDB (`chat_sessions`); los últimos **6** turnos se envían a Woz como JSON compacto (`intent`, `entities`, `confidence`, `reason`, `source`).
- Copy conversacional **grounded** (`GroundedChatCopy`): textos naturales en SDUI usando solo datos reales del backend (saldo, deuda, alias, **nickname** en saludo).
- Errores de payload/ejecución devuelven `event = "chat_error"` + `error`.

### Router de intenciones — Woz (`POST /v1/chat/route`, legacy)

Clasificación vía **Woz** (LLM local Ollama) + fallback heurístico JVM. Variables:

| Variable | Default | Descripción |
|----------|---------|-------------|
| `INTENT_ROUTER_MODE` | `auto` | `auto` \| `woz` \| `heuristic` |
| `WOZ_OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Base de Ollama |
| `WOZ_MODEL` | `qwen2.5:7b-instruct` | Modelo Ollama |
| `WOZ_TIMEOUT_SECONDS` | `60` | Timeout HTTP a Ollama |
| `WOZ_MIN_CONFIDENCE_FOR_ACCEPT` | `0.55` | En `auto`, tras Woz: si baja de este umbral → heurística |
| `INTENT_HEURISTIC_FAST_PATH_MIN_CONFIDENCE` | `0.85` | En `auto`, heurística aceptada sin LLM si supera este umbral |
| `INTENT_HEURISTIC_CONFIG` | (opcional) | Ruta absoluta al JSON heurístico |
| `REPO_ROOT` | (opcional) | Raíz del repo para localizar `local/config/intent_heuristic.json` |

Modo `auto` (**cascade**): heurística primero (fast path si match claro) → Woz si no → heurística otra vez si Woz falla o duda. Detalle: [`docs/intent-routing-contract.md`](../docs/intent-routing-contract.md#cascade-heurística--llm-modo-auto).

**Ollama** hace falta para mensajes que la heurística no resuelve con confianza ≥ fast path. En frases típicas cubiertas por `intent_heuristic.json`, el chat responde sin llamar al LLM. Instalación: [`local/README.md`](../local/README.md).

```bash
ollama pull qwen2.5:7b-instruct
python3 local/scripts/predict_intent_woz.py "paga mi tc"
```

Si no encuentra el JSON heurístico, el proceso falla al arrancar: lanza Gradle
desde `backend/` o define `REPO_ROOT`.

## Endpoints `/v1`

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/auth/login` | No | Login con email/password, devuelve token. |
| `POST` | `/chat/route` | Sí | Clasificación de intención (legacy) |
| `WS` | `/chat/ws` | Sí | **Realtime chat SDUI** (`ChatWsEnvelope`) |
| `GET` | `/me/balance` | Bearer | Saldo de la cuenta del usuario. |
| `GET` | `/me/chat/history` | Bearer | Sesiones de chat del usuario (historial IA). |
| `GET` | `/me/expenses/by-category` | Bearer | Gastos por categoría (`?yearMonth=YYYY-MM`, default mes actual). |
| `GET` | `/me/movements` | Bearer | Movimientos de las tarjetas del usuario. |
| `GET` | `/payments` | Bearer | Historial de pagos (orden descendente). |
| `GET` | `/credit-cards/with-debt` | Bearer | Tarjetas con deuda (`204` si no hay). |
| `GET` | `/credit-cards/{cardId}/movements` | Bearer | Movimientos de una tarjeta. |
| `POST` | `/credit-cards/{cardId}/payments` | Bearer | Pago de tarjeta. Requiere header `Idempotency-Key`. |

Los pagos exigen `Idempotency-Key` para garantizar idempotencia en operaciones
monetarias.

## Pruebas

```bash
./gradlew test
```
