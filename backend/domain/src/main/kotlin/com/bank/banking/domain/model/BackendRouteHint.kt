package com.bank.banking.domain.model

/**
 * Sugerencia de llamada HTTP al API bancario (plantilla de path, sin ejecutar la operación).
 */
data class BackendRouteHint(
    val method: String,
    val pathTemplate: String,
    val requiresAuth: Boolean,
    val requiresIdempotencyKey: Boolean = false,
    /** Si false, la ruta aún no existe en este backend pero la intención sí está definida. */
    val implemented: Boolean = true,
    val description: String,
)
