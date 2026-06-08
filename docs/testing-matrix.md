# Matriz de pruebas — mobile, backend e IA local

Define **qué tests son obligatorios** al añadir o modificar features. Los tests se diseñan
**con** la feature, no después.

## Convenciones generales

| Capa | Framework | Ubicación |
|------|-----------|-----------|
| Mobile shared | kotlin.test + coroutines | `mobile/composeApp/src/commonTest/kotlin/` |
| Backend domain | JUnit 5 | `backend/domain/src/test/kotlin/` |
| Backend application | JUnit 5 + fakes de ports | `backend/application/src/test/kotlin/` |
| Backend API (futuro) | Ktor `testApplication` | `backend/api/src/test/kotlin/` |
| IA local | Scripts CLI + exit code | `local/scripts/simulate_*.py` |
| IA local (futuro) | pytest | `local/tests/` (cuando exista) |

Comando rápido:

```bash
cd mobile && ./gradlew :composeApp:testDebugUnitTest
cd backend && ./gradlew test
python3 local/scripts/simulate_intent_validation.py   # desde raíz del repo
```

---

## Mobile (`com.bank.mobile`)

### Por capa

| Capa | Qué testear | Ejemplo existente |
|------|-------------|-------------------|
| **Domain use case** | Happy path, errores de dominio, validación de entrada | *Pendiente* — seguir patrón backend |
| **Presentation policy** | Umbrales, ramas de decisión puras | `ChatRoutingPolicyTest.kt` |
| **ViewModel** | Transiciones de `UiState`, errores de red simulados | *Pendiente* |
| **Mapper / DTO** | Mapeo bidireccional crítico | *Pendiente* |
| **UI (crítico)** | Login, chat pago TC | *Pendiente* — `androidTest` Compose |

### Features transaccionales

| Feature | Tests mínimos requeridos |
|---------|-------------------------|
| Login | `LoginUseCase`: credenciales válidas / inválidas; `LoginViewModel`: loading → success/error |
| Saldo | `GetBalanceUseCase`: éxito / error de sesión |
| Chat SDUI | `SendChatMessageUseCase` → `/v1/chat/message`; `SduiNodeMapperTest` |
| Pago tarjeta | `PayCreditCardUseCase`: modos; idempotency; botón deshabilitado por mensaje tras pago exitoso; **biometría antes del POST** |
| Política presentación (backend) | Clarificación / soporte | `ChatPresentationPolicyTest.kt` (backend) |

### Patrón recomendado — use case

```kotlin
// commonTest/.../domain/usecase/GetBalanceUseCaseTest.kt
class GetBalanceUseCaseTest {
    private val repo = object : HomeRepository {
        override suspend fun getBalance() = Balance(amount = 100.0, currency = "PEN")
        // ...
    }

    @Test
    fun returnsBalanceOnSuccess() = runTest {
        val result = GetBalanceUseCase(repo).invoke()
        assertEquals(100.0, result.amount)
    }
}
```

---

## Backend (`com.bank.banking`)

### Por capa

| Capa | Qué testear | Ejemplo existente |
|------|-------------|-------------------|
| **Domain model** | Invariantes, operaciones (`Money`) | `MoneyTest.kt` |
| **Use case** | Happy + failure con ports fake | `LoginUseCaseTest.kt`, `RouteChatMessageUseCaseTest.kt` |
| **Route resolver** | Cada `IntentLabel` → hints correctos | *Pendiente* — `BackendRouteResolverTest.kt` |
| **Pay validation** | Modos, expiry, cardholder, fondos | *Pendiente* — `PayCreditCardValidationTest.kt` |
| **Intent classifiers** | Heuristic + fallback | *Pendiente* |
| **API integration** | Auth, idempotency header, 4xx/5xx | *Pendiente* |

### Features transaccionales

| Feature | Tests mínimos requeridos |
|---------|-------------------------|
| Login | Usuario válido, password incorrecto | `LoginUseCaseTest.kt` |
| Chat route | Mensaje vacío, CHECK_BALANCE → GET balance | `RouteChatMessageUseCaseTest.kt` |
| Pago TC | Validación de modos; idempotency replay; fondos insuficientes; tarjeta ajena | *Pendiente* |
| Idempotency | Misma key → mismo resultado; key distinta → nueva operación | *Pendiente* |
| Auth | Token ausente → 401; token inválido → 401 | *Pendiente* (API) |

### Patrón recomendado — use case con port fake

```kotlin
class PayCreditCardUseCaseTest {
    @Test
    fun `rejects custom amount without value`() = runBlocking {
        val uc = PayCreditCardUseCase(/* fake ports */)
        assertThrows<BadRequestException> {
            uc.execute(/* command with CUSTOM and null amount */)
        }
    }
}
```

---

## IA local (Python)

### Validación actual (obligatoria en cambios al clasificador)

| Script | Qué valida | Gate |
|--------|-----------|------|
| `simulate_intent_validation.py` | `eval_v1.jsonl` — intención por frase | Exit 0 = pasa benchmark |
| `simulate_dialogue_validation.py` | `dialogue_eval_v1.jsonl` — intención + next action | Exit 0 = pasa |
| `simulate_intent_validation.py` | Benchmark Woz vs `eval_v1.jsonl` | Exit code 0; requiere Ollama |
| `simulate_dialogue_validation.py` | Multi-turn benchmark | Exit code 0; requiere Ollama |
| `intent_eval.py --provider woz` | Promotion gate + reportes | Revisar `local/reports/` |

Umbrales de promotion: `local/config/thresholds.json` (usado por `intent_eval.py`).

### Tests unitarios Python (recomendado al extraer lógica)

Si mueves helpers reutilizables (p. ej. `decide_next_action`, parsing JSONL) a módulos importables:

```
local/
  tests/
    test_dialogue_policy.py
    test_jsonl_io.py
```

Ejecutar: `pytest local/tests/` (requiere añadir `pytest` a requirements de dev).

---

## SDUI (chat)

| Capa | Tests mínimos | Existente |
|------|---------------|-----------|
| Backend builders | Árbol por intent (0/N tarjetas) | `BalanceUiBuilderTest.kt` |
| Backend policy | Clarificación / soporte | `ChatPresentationPolicyTest.kt` |
| Backend use case | `BuildChatUiUseCase` | *Pendiente* |
| Mobile mapper/renderer | Props → `PayCardChatAction` | `SduiNodeMapperTest.kt` |

Ver `docs/server-driven-ui-blueprint.md`.

---

## CI sugerido (futuro)

```yaml
# .github/workflows/ci.yml (propuesta)
- run: cd backend && ./gradlew test
- run: cd mobile && ./gradlew :composeApp:testDebugUnitTest
- run: python3 local/scripts/simulate_intent_validation.py
```

---

## Definition of Done — feature con dinero

1. Use case backend con ≥ 3 tests (happy, validación, error de negocio).
2. Validación de idempotency testeada.
3. Mobile: use case o ViewModel con tests de confirmación explícita.
4. Sin ejecución de pago sin confirmación del usuario en UI.
5. Audit: `correlationId` o referencia de operación en respuesta.
