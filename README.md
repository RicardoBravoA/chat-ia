# Banking App (KMP) + Asistente IA

App bancaria para Android e iOS con Kotlin Multiplatform, backend KMP/Ktor y un
asistente de chat que enruta operaciones a través de herramientas del backend.
Prioriza seguridad, auditabilidad y confiabilidad.

## Estructura del repo

| Carpeta | Qué contiene | Cómo ejecutar |
|---------|--------------|---------------|
| [`backend/`](backend/README.md) | API Ktor + MongoDB (dominio, aplicación, infraestructura, api). | `cd backend && ./gradlew :api:run` |
| [`mobile/`](mobile/README.md) | App Compose Multiplatform (Android/iOS). | `cd mobile && ./gradlew :composeApp:installDebug` |
| [`local/`](local/README.md) | Tooling Python: clasificador de intenciones, benchmarks y caché. | `source local/venv/bin/activate` |
| [`docs/`](docs/) | Blueprints y runbooks de validación. | — |

## Puesta en marcha rápida

Las tres partes son independientes. Para el flujo completo (móvil → backend → IA):

1. **MongoDB** (requerido por el backend):

   ```bash
   docker run -d --name banking-mongo -p 27017:27017 mongo:7
   ```

2. **Backend** (desde `backend/`, necesita JDK 21):

   ```bash
   ./gradlew :api:run
   ```

   Disponible en `http://localhost:8080` (health: `GET /health`). Detalles y
   endpoints en [`backend/README.md`](backend/README.md).

3. **App móvil** (desde `mobile/`, con el backend arriba):

   ```bash
   ./gradlew :composeApp:installDebug
   ```

   Login demo: `demo@bank.com` / `Demo1234!`. Chat usa SDUI realtime (`WS /v1/chat/ws`).
   Emulador Android: `http://10.0.2.2:8080`; iOS: `http://localhost:8080`.
   Ver [`mobile/README.md`](mobile/README.md).

4. **IA local** (desde la raíz del repo, clasificador de intenciones):

   ```bash
   source local/venv/bin/activate
   python3 local/scripts/train_intent_classifier.py --eval
   python3 local/scripts/simulate_local_chat.py
   ```

   Guía completa de instalación, datasets y ciclo de entrenamiento en
   [`local/README.md`](local/README.md).

## Desarrollo y contexto para agentes

- [`AGENTS.md`](AGENTS.md) — guía de experto KMP, backend, mobile e IA (arquitectura, pruebas, checklist).
- [`.cursor/rules/`](.cursor/rules/) — reglas persistentes de arquitectura, seguridad y testing.
- [`docs/testing-matrix.md`](docs/testing-matrix.md) — qué tests exige cada feature.
- [`docs/intent-routing-contract.md`](docs/intent-routing-contract.md) — contrato intención → API → UI.
- [`docs/server-driven-ui-blueprint.md`](docs/server-driven-ui-blueprint.md) — SDUI para el chat (catálogo, JSON, migración).

## Documentación

- [`docs/presentacion-negocio-chat-ia.md`](docs/presentacion-negocio-chat-ia.md) — presentación orientada a negocio (chat IA, SDUI, modelo local).
- [`docs/presentacion-dev-chat-ia.md`](docs/presentacion-dev-chat-ia.md) — presentación para devs (entrenamiento nube/local, backend, SDUI).
- [`docs/ai-chat-backend-blueprint.md`](docs/ai-chat-backend-blueprint.md) — diseño del chat IA sobre el backend.
- [`docs/local-state-validation-runbook.md`](docs/local-state-validation-runbook.md) — validación local de estados conversacionales.
- [`backend/docs/mongodb-setup.md`](backend/docs/mongodb-setup.md) — instalación y esquema de MongoDB.
