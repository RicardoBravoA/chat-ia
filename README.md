# Banking App (KMP) + Asistente IA

App bancaria para Android e iOS con Kotlin Multiplatform, backend KMP/Ktor y un
asistente de chat que enruta operaciones a través de herramientas del backend.
Prioriza seguridad, auditabilidad y confiabilidad.

## Estructura del repo

| Carpeta | Qué contiene | Cómo ejecutar |
|---------|--------------|---------------|
| [`backend/`](backend/README.md) | API Ktor + MongoDB (dominio, aplicación, infraestructura, api). | `cd backend && ./gradlew :api:run` |
| [`mobile/`](mobile/README.md) | App Compose Multiplatform (Android/iOS). | `cd mobile && ./gradlew :composeApp:installDebug` |
| [`local/`](local/README.md) | Woz: LLM local (Ollama), benchmarks y scripts de validación. | `ollama serve` + scripts en `local/README.md` |
| [`docs/`](docs/) | Blueprints y runbooks de validación. | — |

## Puesta en marcha rápida

Las partes son independientes. El backend arranca sin Ollama; en modo `auto` (default) las frases cubiertas por `local/config/intent_heuristic.json` **no requieren LLM**. Ollama sigue siendo necesario para mensajes ambiguos, follow-ups y benchmarks Woz.

1. **MongoDB** (requerido por el backend):

   ```bash
   docker run -d --name banking-mongo -p 27017:27017 mongo:7
   ```

2. **Ollama + Woz** (para mensajes que la heurística no resuelve y para benchmarks):

   ```bash
   cd /ruta/a/ia

   # macOS: instalador oficial (NO brew install ollama)
   brew uninstall ollama 2>/dev/null || true
   curl -fsSL https://ollama.com/install.sh | sh
   # Si sale "Unable to find application named 'Ollama'" pero existe /Applications/Ollama.app → OK

   open -a Ollama          # o: ollama serve   (dejar corriendo en otra terminal)
   curl -s http://127.0.0.1:11434/            # debe decir "Ollama is running"

   ollama pull qwen2.5:7b-instruct
   python3 local/scripts/predict_intent_woz.py "paga mi tc"   # → PAY_CREDIT_CARD
   ```

   **Ya no hace falta:** venv Python, `pip install`, `train_intent_classifier.py`, ni `.joblib`.
   Detalle y troubleshooting: [`local/README.md`](local/README.md).

3. **Backend** (desde `backend/`, JDK 21):

   ```bash
   ./gradlew :api:run
   ```

   Disponible en `http://localhost:8080` (health: `GET /health`). Detalles y
   endpoints en [`backend/README.md`](backend/README.md).

4. **App móvil** (desde `mobile/`, con MongoDB + backend + Ollama arriba):

   **Android:**

   ```bash
   cd mobile && ./gradlew :composeApp:installDebug
   ```

   **iOS** (macOS + Xcode): configura `TEAM_ID` en `mobile/iosApp/Configuration/Config.xcconfig`, abre `mobile/iosApp/iosApp.xcodeproj` y Run (⌘R). Detalle en [`mobile/README.md`](mobile/README.md).

   Login demo: `woz@bank.com` / `Demo1234!`. Chat usa SDUI realtime (`WS /v1/chat/ws`).
   Emulador Android: `http://10.0.2.2:8080`; iOS simulador: `http://localhost:8080`.

5. **Benchmarks Woz** (opcional, desde la raíz del repo):

   ```bash
   python3 local/scripts/simulate_intent_validation.py
   python3 local/scripts/simulate_woz_chat.py
   ```

## Desarrollo y contexto para agentes

- [`AGENTS.md`](AGENTS.md) — guía de experto KMP, backend, mobile e IA (arquitectura, pruebas, checklist).
- [`.cursor/rules/`](.cursor/rules/) — reglas persistentes de arquitectura, seguridad y testing.
- [`docs/testing-matrix.md`](docs/testing-matrix.md) — qué tests exige cada feature.
- [`docs/intent-routing-contract.md`](docs/intent-routing-contract.md) — contrato intención → API → UI.
- [`docs/server-driven-ui-blueprint.md`](docs/server-driven-ui-blueprint.md) — SDUI para el chat (catálogo, JSON, migración).

## Documentación

- [`docs/guia-arquitectura-proyecto-y-chat-ia.md`](docs/guia-arquitectura-proyecto-y-chat-ia.md) — visión general del repo, capas y flujo del chat IA (diagramas Mermaid).
- [`docs/presentacion-negocio-chat-ia.md`](docs/presentacion-negocio-chat-ia.md) — presentación orientada a negocio (chat IA, SDUI, modelo local).
- [`docs/presentacion-dev-chat-ia.md`](docs/presentacion-dev-chat-ia.md) — presentación para devs (entrenamiento nube/local, backend, SDUI).
- [`docs/ai-chat-backend-blueprint.md`](docs/ai-chat-backend-blueprint.md) — diseño del chat IA sobre el backend.
- [`docs/local-state-validation-runbook.md`](docs/local-state-validation-runbook.md) — validación local de estados conversacionales.
- [`backend/docs/mongodb-setup.md`](backend/docs/mongodb-setup.md) — instalación y esquema de MongoDB.
