# Guía para agentes — experto KMP, backend, mobile e IA

Este repositorio es una **app bancaria real** (Android/iOS + API + clasificador de intenciones).
Actúa como experto en las cuatro áreas. Prioriza **seguridad, arquitectura limpia y pruebas**
sobre velocidad de entrega.

## Rol del agente

| Área | Expertise esperado |
|------|-------------------|
| **Mobile (KMP)** | Clean Architecture en un solo módulo Gradle; Compose Multiplatform; ViewModel + StateFlow; tests en `commonTest`. |
| **Backend (Kotlin JVM)** | Hexagonal: `domain` → `application` → `infrastructure-mongo` → `api` (Ktor); casos de uso testeables; idempotencia en pagos. |
| **IA local (Woz)** | LLM local Ollama; benchmarks JSONL; scripts bajo `local/`; no inventar balances ni ejecutar dinero. |
| **Orquestación chat** | Backend clasifica intención; mobile aplica política de UI; operaciones monetarias solo con confirmación explícita vía API. |
| **SDUI (chat)** | Backend `BuildChatUiUseCase` + builders; mobile `SduiRenderer`; endpoint `POST /v1/chat/message`. |

## Mapa del repositorio

```
ia/
├── mobile/          # KMP Compose — módulo :composeApp
├── backend/         # Ktor + MongoDB — domain, application, infrastructure-mongo, api
├── local/           # Woz: scripts Python, benchmarks, config heurística
├── docs/            # Blueprints, runbooks, contratos
└── .cursor/rules/   # Reglas persistentes para el agente
```

## Principios transversales (obligatorios)

1. **Arquitectura por capas** — reglas de negocio en domain/use cases; UI sin lógica transaccional; API solo valida y delega.
2. **Tests con el diseño** — cada feature nueva incluye tests unitarios antes o junto al código (ver `docs/testing-matrix.md`).
3. **Flujos monetarios** — confirmación explícita, `Idempotency-Key`, validación en backend; nunca ejecutar pagos desde el clasificador de intenciones.
4. **Código mínimo y coherente** — reutiliza patrones existentes; no sobre-abstraigas; match naming y estilo del módulo.
5. **Sin secretos en logs** — no loguear tokens, PAN, passwords ni datos sensibles.

## Mobile — cómo trabajar

- **Módulo:** `:composeApp` con capas por paquete (`com.bank.mobile.domain|data|presentation`).
- **Patrón:** use case → repository (interface en domain, impl en data) → ViewModel con `StateFlow<*UiState>`.
- **DI:** manual en `BankingApp.kt`; nuevas dependencias se cablean ahí.
- **UI:** atomic design (`presentation.ui.atoms|molecules|organisms`); navegación en `presentation.navigation`.
- **Tests:** `composeApp/src/commonTest/` — domain use cases, ViewModels, políticas (`ChatRoutingPolicy`).
- **Platform:** `expect/actual` solo para URL backend, URLs externas, galería.

Ver: `.cursor/rules/10-mobile-architecture.mdc`, `.cursor/rules/15-mobile-package-conventions.mdc`.

## Backend — cómo trabajar

- **Stack:** Kotlin JVM 21, Ktor, MongoDB (no es KMP compartido con mobile).
- **Nuevo endpoint:** DTO en `api/dto` → ruta en `BankingV1Routes.kt` → use case en `application` → port en `domain` → adapter en `infrastructure-mongo` → wire en `BankingCompositionRoot.kt`.
- **Pagos:** siempre exigir header `Idempotency-Key`; validar en `PayCreditCardValidation.kt`.
- **Chat:** `RouteChatMessageUseCase` + `IntentClassifierPort`; mapeo en `BackendRouteResolver`.
- **Tests:** JUnit 5 en `domain/src/test` y `application/src/test`; fake ports, no Mongo en unit tests.

Ver: `.cursor/rules/20-backend-architecture.mdc`, `.cursor/rules/25-backend-api-conventions.mdc`.

## IA local — cómo trabajar

- **Todo Python vive en `local/`** — scripts de benchmark (stdlib; venv opcional).
- **Ollama en marcha** antes de probar chat o benchmarks: `ollama serve` + `ollama pull qwen2.5:7b-instruct`.
- **Comandos desde la raíz del repo:** `python3 local/scripts/...`
- **Validar Woz:** `simulate_intent_validation.py`, `intent_eval.py --provider woz` (Ollama en marcha).
- **No sustituir backend** — Woz clasifica intención; use cases ejecutan operaciones bancarias.

Ver: `.cursor/rules/55-local-ai-tooling.mdc`, `docs/intent-routing-contract.md`.

## Checklist antes de proponer un PR

- [ ] Tests unitarios para lógica nueva (domain, application, presentation o scripts).
- [ ] Happy path + al menos un failure path en flujos transaccionales.
- [ ] Sin lógica de negocio en Composables ni en scripts Python de inferencia.
- [ ] Idempotencia y auth verificados en endpoints de dinero.
- [ ] **README validados/actualizados** (ver `.cursor/rules/05-readme-documentation-sync.mdc`): raíz, módulo tocado y `docs/` si aplica.
- [ ] Benchmark de intenciones ejecutado si tocaste datasets o clasificador.

## Documentación de referencia

| Documento | Contenido |
|-----------|-----------|
| [README.md](README.md) | Puesta en marcha rápida |
| [docs/testing-matrix.md](docs/testing-matrix.md) | Qué tests exige cada feature |
| [docs/intent-routing-contract.md](docs/intent-routing-contract.md) | Contrato intención → API → UI |
| [docs/server-driven-ui-blueprint.md](docs/server-driven-ui-blueprint.md) | SDUI: catálogo, JSON, migración del chat |
| [docs/ai-chat-backend-blueprint.md](docs/ai-chat-backend-blueprint.md) | Diseño objetivo del chat |
| [local/README.md](local/README.md) | Tooling Python |
| [backend/README.md](backend/README.md) | API y arranque |
| [mobile/README.md](mobile/README.md) | App móvil |

## Reglas Cursor (`.cursor/rules/`)

Siempre aplican: `00-product-context`, `05-readme-documentation-sync`, `10-mobile`, `20-backend`, `30-security`, `40-ai-chat`, `50-testing`, `60-server-driven-ui-principles`.
Por archivo/glob: `15-mobile-package`, `25-backend-api`, `35-intent-routing`, `55-local-ai`, `61-sdui-backend`, `62-sdui-mobile`.
Siempre (principios SDUI): `60-server-driven-ui-principles`.
