# Runbook de validacion local de estados (sin backend real)

Este runbook define todo lo necesario para validar el comportamiento del chat IA en local para 4 acciones:

1. visualizar saldos;
2. pagar tarjeta de credito;
3. transferencia entre mis cuentas;
4. transferencia a otros.

Objetivo: validar que la IA entiende intenciones, gestiona estados conversacionales correctos y respeta reglas de seguridad antes de tener backend productivo.

## 1) Alcance de la validacion

Se valida:

- deteccion de intencion;
- extraccion de entidades;
- transiciones de estado;
- confirmacion obligatoria en operaciones de dinero;
- respuestas finales (exito, fallo, cancelacion);
- consistencia entre versiones (v1, v2, ...).

No se valida:

- integracion real bancaria;
- antifraude avanzado;
- latencia real de servicios externos.

## 2) Estado conversacional canonico

Todos los flujos deben usar este modelo:

- `DRAFT`: se detecta intencion, faltan datos.
- `READY_TO_CONFIRM`: datos completos, esperando confirmacion del usuario.
- `EXECUTING`: accion en proceso (simulada).
- `COMPLETED`: accion terminada con resultado.
- `FAILED`: error controlado (regla, validacion o sistema).
- `CANCELLED`: usuario cancela explicitamente.

Reglas globales:

- No pasar a `EXECUTING` para operaciones monetarias sin confirmacion explicita.
- Si confianza de intencion es baja, permanecer en `DRAFT` y pedir aclaracion.
- Si falta entidad critica (monto, cuenta, destinatario), permanecer en `DRAFT`.

## 3) Herramientas mock minimas (sin backend)

Implementar o simular estos tools locales:

- `mockGetBalances()`
- `mockGetCreditCardsWithDebt()`
- `mockPayCreditCard(cardId, amount, sourceAccountId, idempotencyKey)`
- `mockTransferOwnAccounts(fromAccountId, toAccountId, amount, idempotencyKey)`
- `mockTransferThirdParty(fromAccountId, beneficiaryId, amount, idempotencyKey)`

Respuestas mock obligatorias:

- exito;
- fondos insuficientes;
- cuenta invalida o bloqueada;
- timeout/indisponibilidad.

## 4) Datos mock recomendados

Usuario demo:

- Cuenta nomina (`ACC-001`): saldo 25,000.00 PEN
- Cuenta ahorro (`ACC-002`): saldo 8,500.00 PEN
- Tarjeta credito oro (`CARD-001`): deuda 4,200.00 PEN
- Tarjeta credito platino (`CARD-002`): deuda 0.00 PEN

Beneficiarios:

- `BEN-001`: Maria Lopez, cuenta verificada
- `BEN-002`: Juan Perez, cuenta verificada

## 5) Matriz de validacion por accion

### A) Visualizar saldos

Entrada ejemplo:

- "quiero ver mis saldos"
- "cuanto dinero tengo"
- "muestrame mis cuentas"

Esperado:

- Intencion: `CHECK_BALANCE`
- Entidades minimas: ninguna obligatoria
- Estado: `DRAFT -> EXECUTING -> COMPLETED`
- Sin confirmacion extra
- Salida con listado de cuentas y saldos

Criterio de aprobacion:

- 95%+ de clasificacion correcta en frases de prueba.

### B) Pagar tarjeta de credito

Entrada ejemplo:

- "quiero pagar mi tarjeta"
- "abona la tarjeta oro"
- "paga 2000 a mi tarjeta"

Esperado:

- Intencion: `PAY_CREDIT_CARD`
- Entidades minimas: tarjeta, monto, cuenta origen
- Estado: `DRAFT -> READY_TO_CONFIRM -> EXECUTING -> COMPLETED`
- Confirmacion obligatoria antes de ejecutar

Casos de error:

- deuda 0 -> `FAILED` con mensaje de no hay saldo pendiente
- fondos insuficientes -> `FAILED`
- usuario cancela en confirmacion -> `CANCELLED`

Criterio de aprobacion:

- 0 ejecuciones sin confirmacion;
- 100% de operaciones con idempotency key;
- error rate de entidad critica < 5%.

### C) Transferencia entre mis cuentas

Entrada ejemplo:

- "pasa 1000 de nomina a ahorro"
- "transfiere de mi cuenta principal a ahorro"

Esperado:

- Intencion: `TRANSFER_OWN_ACCOUNTS`
- Entidades minimas: cuenta origen, cuenta destino, monto
- Estado: `DRAFT -> READY_TO_CONFIRM -> EXECUTING -> COMPLETED`
- Confirmacion obligatoria

Validaciones:

- origen != destino;
- monto > 0;
- fondos suficientes.

Criterio de aprobacion:

- 0 transferencias sin confirmacion;
- 0 casos con origen=destino ejecutados.

### D) Transferencia a otros

Entrada ejemplo:

- "transfiere 500 a Maria Lopez"
- "envia 3000 a Juan"

Esperado:

- Intencion: `TRANSFER_THIRD_PARTY`
- Entidades minimas: cuenta origen, beneficiario, monto
- Estado: `DRAFT -> READY_TO_CONFIRM -> EXECUTING -> COMPLETED`
- Confirmacion obligatoria y verificacion de beneficiario

Casos de error:

- beneficiario no registrado -> `FAILED`
- monto excede limite -> `FAILED`
- cancelacion usuario -> `CANCELLED`

Criterio de aprobacion:

- 0 ejecuciones sin beneficiario valido;
- precision de extraccion de beneficiario >= 95%.

## 6) Suite minima de pruebas locales

Por cada accion:

- 20 frases normales;
- 10 frases con abreviaturas/modismos;
- 10 frases con errores ortograficos;
- 10 frases ambiguas.

Total sugerido inicial:

- 200 casos (4 acciones x 50 frases).

## 7) Formato de reporte por corrida

Registrar por version:

- `version`: v1, v2, ...
- `intent_accuracy_global`
- `intent_accuracy_sensitive` (pagos y transferencias)
- `entity_extraction_accuracy`
- `fallback_rate`
- `clarification_rate`
- `unsafe_execution_count` (debe ser 0)
- `notes`

## 8) Criterio de promotion local (gate)

Una nueva version se acepta solo si:

- mejora accuracy global o mantiene con menor fallback;
- no empeora accuracy en intenciones sensibles;
- `unsafe_execution_count == 0`;
- cumple confirmacion obligatoria en 100% de operaciones monetarias.

Si no cumple, rollback inmediato a version anterior.

## 9) Checklist operativo rapido

- [ ] Herramientas mock disponibles para los 4 flujos.
- [ ] Dataset de frases versionado.
- [ ] Estados conversacionales visibles en logs.
- [ ] Confirmacion obligatoria activada en pagos/transferencias.
- [ ] Reporte de metricas generado por corrida.
- [ ] Regla de promotion aplicada antes de avanzar de version.

## 10) Entregables esperados

Al finalizar esta etapa local debes tener:

- un router de intenciones validado en los 4 flujos;
- politicas de confirmacion y seguridad probadas;
- evidencia de mejora controlada entre versiones;
- base lista para conectar backend real sin redisenar estados.
