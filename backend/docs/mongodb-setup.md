# MongoDB: instalación y esquema (backend banking)

En MongoDB no existen **tablas**; el equivalente son **colecciones**. Los documentos se crean al primer `insert`; no hace falta un `CREATE TABLE`. Este proyecto usa la base de datos **`banking`** por defecto y crea **índices** y **datos demo** al arrancar el servidor (ver `MongoBootstrap`).

---

## Qué instalar

### Opción A — Docker (recomendada para desarrollo)

1. Instala [Docker Desktop](https://www.docker.com/products/docker-desktop/) (macOS / Windows) o Docker Engine (Linux).
2. Levanta un contenedor con MongoDB 7.x (ejemplo):

```bash
docker run -d --name banking-mongo -p 27017:27017 mongo:7
```

La URI por defecto del backend es `mongodb://127.0.0.1:27017` (sin usuario/contraseña en local).

### Opción B — Instalación nativa

| Sistema | Cómo |
|--------|------|
| **macOS (Homebrew)** | `brew tap mongodb/brew && brew install mongodb-community@7.0` y luego seguir la guía de brew para arrancar el servicio. |
| **Linux** | Paquete oficial o [instrucciones MongoDB](https://www.mongodb.com/docs/manual/installation/) para tu distribución. |
| **Windows** | [MongoDB Community Server](https://www.mongodb.com/try/download/community) (MSI). |

Asegúrate de que el proceso escuche en el puerto **27017** (o ajusta `MONGO_URI`).

### Herramientas opcionales

- **`mongosh`**: shell de MongoDB (viene con muchas instalaciones o se instala aparte).
- **MongoDB Compass**: cliente gráfico para inspeccionar bases y colecciones.

---

## Base de datos y conexión

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `MONGO_URI` | `mongodb://127.0.0.1:27017` | Cadena de conexión (host/puerto; usuario/clave si aplica). |
| Nombre de BD en código | `banking` | Definido en `BankingCompositionRoot` (`databaseName = "banking"`). |

Ejemplo con autenticación (producción):

```text
mongodb://usuario:password@host:27017/?authSource=admin
```

El driver creará la base `banking` en el primer uso.

---

## Colecciones que usa el backend

No necesitas crearlas a mano: el **primer insert** o el **bootstrap** las crean. Los nombres están en `MongoCollections.kt`.

| Colección | Rol |
|-----------|-----|
| `users` | Credenciales: `_id`, `email`, `passwordHash`. |
| `sessions` | Sesiones de login: token/expiración (campos según `MongoSessionRepository`). |
| `accounts` | Cuenta por usuario: `amount`, `currency`, `userId`. |
| `credit_cards` | Tarjetas: **`debt`** (deuda total), **`creditLine`** (línea de crédito máx.), **`currency`**, `userId`, `alias`, `lastFourDigits`. |
| `card_movements` | Movimientos: `cardId`, `kind`, **`amount`** (importe del movimiento), `currency`, `description`, `occurredAt`. |

Los importes van en **unidades mínimas** (p. ej. centavos); `debt` ≤ `creditLine` en datos consistentes.

### Migrar documentos viejos en MongoDB Compass

Si aún tienes `outstandingMinor` en las tarjetas:

```javascript
use banking
db.credit_cards.updateMany(
  { outstandingMinor: { $exists: true } },
  [
    { $set: { debt: "$outstandingMinor", creditLine: 300000 } },
    { $unset: "outstandingMinor" }
  ]
)
```

Ajusta `creditLine` (unidades mínimas) al límite real de cada producto; debe ser **≥** la deuda actual.

En **`card_movements`**, renombra el campo en `mongosh` (Compass → pestaña *mongosh*):

```javascript
use banking
db.card_movements.updateMany(
  { amount: { $exists: true } },
  { $rename: { amount: "amount" } }
)
```

3. Reinicia la API y prueba `GET /v1/credit-cards/with-debt` y pagos.
| `idempotency` | Claves de idempotencia de pagos: `_id` = clave, `userId`, `result`, `createdAt`. |

---

## Índices que crea el arranque (`MongoBootstrap`)

Al iniciar el servidor se ejecuta el bootstrap (si la colección `users` está vacía, también inserta datos demo):

| Colección | Índice | Notas |
|-----------|--------|--------|
| `sessions` | `expiresAt` ascendente | Consultas por expiración. |
| `idempotency` | `userId` ascendente | **No** único (varias claves por usuario). |

La colección `idempotency` usa **`_id`** como clave de idempotencia; MongoDB garantiza unicidad en `_id`, lo que evita duplicar la misma operación.

No hace falta ejecutar scripts SQL ni crear “tablas” vacías antes de levantar la API.

---

## Datos demo (seed)

Si **`users`** está vacía al arrancar, se inserta:

- Usuario: **`demo@bank.com`** / **`Demo1234!`**
- Cuenta, tarjeta `card_demo_1` y un movimiento de ejemplo.

Si ya hay al menos un usuario, el seed de demo **no** se ejecuta (solo se aseguran los índices).

---

## Transacciones y replica set

El pago entre cuenta, tarjeta y movimiento está implementado como **actualizaciones secuenciales** con rollback aproximado si falla una fase (`MongoPaymentExecutionGateway`). Para **transacciones multi-documento** oficiales en MongoDB necesitas un **replica set** (incluso un nodo único en desarrollo). En local con un solo `mongod` sin replica set, las transacciones de sesión no están disponibles; el diseño actual no depende de ellas.

---

## Comprobar que todo corre

1. MongoDB escuchando en `27017`.
2. Desde la raíz del proyecto backend:

```bash
./gradlew :api:run
```

3. Probar: `GET http://localhost:8080/health` (puerto por defecto; configurable con `PORT`).

---

## Error: “trying to access MongoDB over HTTP on the native driver port”

Ese texto lo devuelve **MongoDB cuando abres `http://localhost:27017` en el navegador**. El puerto **27017** es para el **protocolo del driver** (wire protocol), no para HTTP. El navegador no puede “ver” la base de datos como una página web.

**Qué hacer en su lugar:**

- **Comprobar que Mongo está vivo:** en terminal, `mongosh "mongodb://127.0.0.1:27017"` (o Compass con la misma URI).
- **Probar tu API:** `http://localhost:8080/health` (Ktor), no el puerto 27017 en el browser.
- **URI en el backend:** `MONGO_URI=mongodb://127.0.0.1:27017` (sin `http://`).

Si ves ese mensaje **sin haber abierto el navegador**, entonces algo está enviando peticiones HTTP al 27017; revisa que ningún cliente esté usando URL `http://...:27017` en lugar de `mongodb://...:27017`.

---

## Resumen

| Pregunta | Respuesta |
|----------|-----------|
| ¿Qué instalar? | MongoDB (Docker o binario oficial) + opcionalmente `mongosh` o Compass. |
| ¿Qué “tablas” crear? | Ninguna manualmente: usa **colecciones** listadas arriba; se crean al usar la API o el bootstrap. |
| ¿Índices? | Los crea el servidor al arrancar (`MongoBootstrap`). |
| ¿Datos iniciales? | Seed automático si `users` está vacía. |
