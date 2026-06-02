# Mobile (KMP Android/iOS)

Aplicacion Kotlin Multiplatform con Compose Multiplatform y enfoque Clean Architecture por capas (domain/data/presentation en `commonMain`).

## Funcionalidad incluida

- Login contra `POST /v1/auth/login` (pantalla con fondo blanco).
- Bottom bar con dos tabs: `Inicio` y `Chat`.
- Inicio:
  - Consulta saldo (`GET /v1/me/balance`).
  - Historial de pagos (`GET /v1/payments`, orden descendente en backend).
- Chat (SDUI):
  - UI tipo conversacion (fondo beige, burbujas, barra inferior).
  - Realtime con `WS /v1/chat/ws` — mobile envía `{"message":"..."}` y recibe `ChatWsEnvelope` con `uiTree`; render en `presentation/sdui/SduiRenderer.kt`.
  - Mientras responde backend, se muestra indicador animado de escritura (`...`).
  - Pago de tarjeta desde `PayCardChatPanel` → `POST /v1/credit-cards/{cardId}/payments` con `Idempotency-Key`.
  - Tras un pago exitoso, el botón **"Pagar ahora"** se deshabilita en ese mensaje; un **nuevo** mensaje en el chat (p. ej. "pagar tc") trae un panel nuevo habilitado.

### Credenciales demo (backend con seed)

| Campo | Valor |
|-------|--------|
| Email | `demo@bank.com` |
| Password | `Demo1234!` |

## Ejecutar Android

Desde `mobile/` (con backend en `http://localhost:8080`):

```bash
./gradlew :composeApp:installDebug
```

- Emulador Android: `http://10.0.2.2:8080`
- Dispositivo físico: IP de tu máquina en la red local (no `localhost`)

## Ejecutar iOS

Genera y abre proyecto iOS desde Xcode usando el framework `ComposeApp` de `composeApp`.
La app iOS usa `http://localhost:8080`.

## Pruebas

```bash
./gradlew :composeApp:testDebugUnitTest
```

## Troubleshooting

| Síntoma | Causa habitual |
|---------|----------------|
| `404`/fallo al abrir `WS /v1/chat/ws` | Backend sin reiniciar tras actualizar código o servidor viejo — ejecutar de nuevo `./gradlew :api:run` en `backend/`. |
| `401` en chat | Sesión expirada — volver a login. |
