# AI Chat + Backend Blueprint (Banca)

Este documento define como implementar un chat con IA para operaciones bancarias de forma segura, escalable y auditable.

## 1) Objetivo

Permitir que el usuario pida acciones bancarias en lenguaje natural (por ejemplo, "quiero pagar mi tarjeta") y que el sistema:

1. entienda la intencion correctamente;
2. consulte datos reales en backend;
3. pida confirmacion explicita en operaciones sensibles;
4. ejecute la transaccion de forma segura;
5. entregue comprobante y rastro de auditoria.

## 2) Principio clave

La IA no es la autoridad bancaria. La autoridad siempre es el backend.

- IA: entender y orquestar.
- Backend: validar reglas y ejecutar operaciones.

## 3) Arquitectura recomendada

Componentes:

- Mobile App (KMM + Compose): interfaz de chat y estado de conversacion.
- AI Gateway (KMP/Ktor): capa orquestadora con politicas y seguridad.
- Banking API (KMP/Ktor): cuentas, tarjetas, pagos, movimientos.
- Audit Service: logs estructurados y evidencia de cada accion.
- MongoDB: historial de conversaciones, eventos de orquestacion, auditoria.
- Redis: idempotencia, rate limiting, contexto de sesion de corto plazo.

Flujo:

1. app envia mensaje del usuario al AI Gateway;
2. AI Gateway clasifica intencion y extrae entidades;
3. AI Gateway invoca tools del backend para obtener datos reales;
4. IA responde y solicita confirmacion si hay movimiento de dinero;
5. AI Gateway ejecuta operacion mediante endpoint transaccional;
6. backend responde resultado + folio;
7. AI Gateway devuelve respuesta final al chat y registra auditoria.

## 4) Como "aprende" distintas formas de pedir lo mismo

No se recomienda aprendizaje libre en produccion para banca. El enfoque correcto es aprendizaje supervisado y versionado.

### 4.1 Catalogo de intenciones (estable)

Definir intenciones canonicas:

- CHECK_BALANCE
- LIST_MOVEMENTS
- PAY_CREDIT_CARD
- TRANSFER_MONEY
- BLOCK_CARD

### 4.2 Dataset de frases equivalentes

Para cada intencion, mantener ejemplos de frases reales:

- "quiero pagar mi tarjeta"
- "abonar tc"
- "liquidar la de credito"
- "paga lo que debo en tarjeta"

Tambien incluir:

- errores ortograficos;
- abreviaturas;
- frases ambiguas;
- mezcla de espanol formal/informal.

### 4.3 Clasificacion + extracción de entidades

Dos opciones validas:

- clasificador de intencion con embeddings + similitud;
- LLM con salida estructurada (JSON schema estricto).

Recomendacion inicial:

- usar LLM con schema estricto para velocidad de iteracion;
- pasar a clasificador dedicado cuando crezca el trafico.

Entidades a extraer:

- cuenta origen
- tarjeta objetivo
- monto
- fecha
- moneda

### 4.4 Bucle de mejora continua (learning loop)

1. registrar mensajes anonimizados + intencion predicha + resultado;
2. detectar fallos (intencion incorrecta, fallback, cancelaciones);
3. revisar muestras periodicamente (humano + reglas);
4. actualizar ejemplos y politicas;
5. versionar cambios (intent-model-v1, v2, ...);
6. evaluar en set de pruebas antes de publicar.

### 4.5 Aprendizaje online en vivo

Evitar que el modelo cambie comportamiento automaticamente en produccion sin control humano.

- permitido: ajustar prompts y ejemplos con despliegue controlado;
- no permitido: auto-reentrenar y publicar sin aprobacion.

## 5) Contrato de seguridad para operaciones monetarias

Antes de ejecutar pagos o transferencias:

- sesion valida y no expirada;
- autorizacion del usuario para la accion;
- confirmacion explicita en el chat;
- idempotency key por solicitud;
- step-up auth (OTP/biometria) segun riesgo;
- validaciones backend de saldo, deuda, limites y estado de cuenta.

## 6) Estados conversacionales recomendados

- DRAFT: se entiende intencion, faltan datos.
- READY_TO_CONFIRM: datos completos, esperando confirmacion.
- EXECUTING: operacion en curso en backend.
- COMPLETED: operacion finalizada con comprobante.
- FAILED: error controlado con mensaje seguro y accion sugerida.

## 7) Observabilidad y auditoria

Registrar por cada turno:

- correlationId
- userId (anonimizado para analitica)
- intencion detectada
- tools invocadas
- decision de confirmacion
- resultado de backend
- tiempo de respuesta y errores

Metricas minimas:

- intent accuracy
- fallback rate
- confirmation-to-success rate
- abandonment rate
- false positive de intenciones sensibles

## 8) Que necesitas instalar (si aplica)

Se puede avanzar mucho desde este IDE sin instalar nada extra para documentar, disenar y estructurar reglas.

Para implementar y ejecutar de extremo a extremo normalmente si necesitaras:

- JDK 17+;
- Android Studio + SDK Android;
- Xcode (si haras iOS en macOS);
- Kotlin/Gradle toolchain;
- Docker (opcional, recomendado para servicios locales);
- MongoDB (local o Atlas);
- Redis (local o gestionado);
- proveedor LLM/API para tool calling.

Nota:

- Cursor/IDE ayuda a construir y automatizar.
- El "aprendizaje" del chat depende de tu backend, datasets y pipeline de evaluacion, no del IDE por si solo.

## 9) Fases de implementacion recomendadas

Fase 1:

- intenciones base (saldo, movimientos, pago de tarjeta);
- confirmacion explicita;
- tool calling con backend;
- auditoria minima.

Fase 2:

- mejoras de clasificacion;
- step-up auth adaptativa;
- evaluaciones automatizadas de prompts/intenciones.

Fase 3:

- ampliar intenciones (transferencias, bloqueo, disputas);
- optimizar precision y tiempos;
- gobierno de cambios de IA (versiones, aprobaciones, rollback).

## 10) Definition of Done por accion IA

Una accion se considera lista solo si cumple:

- intencion detectada con precision aceptable;
- validaciones de seguridad activas;
- confirmacion requerida en operaciones sensibles;
- pruebas unitarias/integracion/UI segun aplique;
- evidencia de auditoria completa;
- manejo de errores y mensajes claros al usuario.
