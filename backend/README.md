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

- Cliente envía `ChatMessageRequest` JSON (`{"message":"..."}`).
- Backend responde `ChatWsEnvelope` con `event = "chat_response"` y `response` (`ChatMessageResponse` con `uiTree`).
- Errores de payload/ejecución devuelven `event = "chat_error"` + `error`.
- Simulación de latencia de bot: delay aleatorio entre **1 y 3 segundos** por mensaje.

### Router de intenciones (`POST /v1/chat/route`, legacy)

Clasificación + `suggestedActions` (sin `uiTree`). Usado por eval de intents en `local/`. Variables del clasificador (ver
[`local/README.md`](../local/README.md)):

- `INTENT_ROUTER_MODE`: `auto` \| `python` \| `heuristic`.
- `REPO_ROOT`: raíz del repo para localizar el modelo y `intent_heuristic.json`.
- `PYTHON_BIN`: intérprete para `local/scripts/predict_intent.py`.
- `INTENT_HEURISTIC_CONFIG` (opcional): ruta absoluta al JSON heurístico.

Si no encuentra el JSON heurístico, el proceso falla al arrancar: lanza Gradle
desde `backend/` o define `REPO_ROOT`.

## Endpoints `/v1`

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/auth/login` | No | Login con email/password, devuelve token. |
| `POST` | `/chat/route` | Sí | Clasificación de intención (legacy) |
| `WS` | `/chat/ws` | Sí | **Realtime chat SDUI** (`ChatWsEnvelope`) |
| `GET` | `/me/balance` | Bearer | Saldo de la cuenta del usuario. |
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
