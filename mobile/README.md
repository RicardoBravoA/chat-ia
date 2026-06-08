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
  - Realtime con `WS /v1/chat/ws` — mobile envía `{"message":"...","sessionId":"..."}`; los chips del `GreetingCard` añaden `selectedIntent` para ir directo al SDUI sin re-clasificar (saldo, pago TC, gastos del mes, historial chat).
  - SDUI nuevos: `ChatHistoryRow` (sesiones previas) y `SpendingCategoryRow` (categoría + transacciones + total).
  - El backend devuelve textos conversacionales **grounded** (datos reales) encima de tarjetas SDUI.
  - Mientras responde backend, se muestra indicador animado de escritura (`...`).
  - Pago de tarjeta desde `PayCardChatPanel` → confirmación **biométrica o bloqueo del dispositivo** → `POST /v1/credit-cards/{cardId}/payments` con `Idempotency-Key`.
  - Tras un pago exitoso, el botón **"Pagar ahora"** se deshabilita en ese mensaje; un **nuevo** mensaje en el chat (p. ej. "pagar tc") trae un panel nuevo habilitado.

### Credenciales demo (backend con seed)

| Campo | Valor |
|-------|--------|
| Email | `woz@bank.com` |
| Password | `Demo1234!` |

## Ejecutar Android

Desde `mobile/` (con backend en `http://localhost:8080`):

```bash
./gradlew :composeApp:installDebug
```

- Emulador Android: `http://10.0.2.2:8080`
- Dispositivo físico: IP de tu máquina en la red local (no `localhost`)

## Ejecutar iOS

Requisitos: macOS, Xcode 15+ y backend en `http://localhost:8080`.

1. Configura tu Apple Team ID en `iosApp/Configuration/Config.xcconfig` (`TEAM_ID=...`).
2. Abre el proyecto Xcode:

   ```bash
   open iosApp/iosApp.xcodeproj
   ```

3. Selecciona el target **iosApp** y un simulador o dispositivo.
4. Run (⌘R). Xcode ejecuta `:composeApp:embedAndSignAppleFrameworkForXcode` antes de compilar Swift.

Alternativa CLI (solo framework KMP, sin instalar en simulador):

```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

- Simulador iOS: `http://localhost:8080` (ver `PlatformConfig.ios.kt`).
- Dispositivo físico: cambia `backendBaseUrl()` a la IP de tu máquina en la red local.

`Info.plist` incluye `NSFaceIDUsageDescription` (pagos con biometría) y `NSAllowsLocalNetworking` para HTTP local en desarrollo.

## Step-up biométrico (operaciones con dinero)

Antes de ejecutar un pago, mobile pide autenticación local:

| Plataforma | Comportamiento |
|------------|----------------|
| Android | `BiometricPrompt`: huella/rostro fuerte o PIN/patrón del dispositivo (`androidx.biometric`) |
| iOS | `LocalAuthentication`: Face ID / Touch ID con fallback a código del dispositivo (`LAPolicyDeviceOwnerAuthentication`) |

Si el usuario cancela, no se llama al backend. Si no hay bloqueo configurado, se muestra un mensaje de error en el chat.

Implementación: `ConfirmSensitiveActionUseCase` + `platform/createBiometricAuthenticator()`.

## Pruebas

```bash
./gradlew :composeApp:testDebugUnitTest
```

## Troubleshooting

| Síntoma | Causa habitual |
|---------|----------------|
| `404`/fallo al abrir `WS /v1/chat/ws` | Backend sin reiniciar tras actualizar código o servidor viejo — ejecutar de nuevo `./gradlew :api:run` en `backend/`. |
| `401` en chat | Sesión expirada — volver a login. |
| iOS: `build.db: database is locked` | Dos builds a la vez (Xcode + `./gradlew` en terminal). Para el build, cierra Xcode y borra DerivedData: `rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*` |
| iOS: Gradle/Kotlin falla al compilar framework | `xcode-select` debe apuntar a Xcode completo: `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer` |
| iOS: warning `Compile Kotlin` / sin outputs | Esperado en KMP; el script `embedAndSignAppleFrameworkForXcode` debe ejecutarse en cada build (`alwaysOutOfDate` en el proyecto). |

## Logs de depuración (Android Logcat / consola iOS)

- **REST** (`GET/POST`): plugin oficial [`ktor-client-logging`](https://ktor.io/docs/client-logging.html) en `createBankingHttpClient()` con `LogLevel.BODY`. Formato estilo OkHttp (`--> GET`, headers, body, `<-- 200`). `Authorization`, `Idempotency-Key` y `"password"` en JSON se enmascaran.
- **WebSocket** (`/v1/chat/ws`): `BankingWsOkHttpLogger` — mismo estilo para frames TEXT (`--> WS TEXT`, `<-- WS TEXT` con latencia y body JSON con `sessionId`, `metadata`, `uiTree`).

Filtra en Logcat por `-->`, `<--` o `WS TEXT`.
