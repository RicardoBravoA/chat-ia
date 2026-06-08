package com.bank.banking.application

import com.bank.banking.domain.model.BackendRouteHint
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel

/**
 * Mapea la intención detectada a uno o más endpoints del API (solo sugerencia; no ejecuta dinero).
 */
object BackendRouteResolver {
    fun resolve(classification: IntentClassification): List<BackendRouteHint> {
        return when (classification.intent) {
            IntentLabel.CHECK_BALANCE ->
                listOf(
                    BackendRouteHint(
                        method = "GET",
                        pathTemplate = "/v1/me/balance",
                        requiresAuth = true,
                        requiresIdempotencyKey = false,
                        implemented = true,
                        description = "Obtener saldo de la cuenta asociada a la sesión",
                    ),
                )
            IntentLabel.PAY_CREDIT_CARD ->
                listOf(
                    BackendRouteHint(
                        method = "GET",
                        pathTemplate = "/v1/credit-cards/with-debt",
                        requiresAuth = true,
                        requiresIdempotencyKey = false,
                        implemented = true,
                        description = "Listar tarjetas con deuda e importes (mínimo, pago del mes, titular, vencimiento)",
                    ),
                    BackendRouteHint(
                        method = "POST",
                        pathTemplate = "/v1/credit-cards/{cardId}/payments",
                        requiresAuth = true,
                        requiresIdempotencyKey = true,
                        implemented = true,
                        description = "Pagar tarjeta: body con titular, vencimiento, paymentMode (MINIMUM|STATEMENT_MONTH|FULL_DEBT|CUSTOM) y amount si CUSTOM",
                    ),
                )
            IntentLabel.TRANSFER_OWN_ACCOUNTS ->
                listOf(
                    BackendRouteHint(
                        method = "POST",
                        pathTemplate = "/v1/transfers/own",
                        requiresAuth = true,
                        requiresIdempotencyKey = true,
                        implemented = false,
                        description = "Transferencia entre cuentas propias — endpoint pendiente en este backend",
                    ),
                )
            IntentLabel.TRANSFER_THIRD_PARTY ->
                listOf(
                    BackendRouteHint(
                        method = "POST",
                        pathTemplate = "/v1/transfers/third-party",
                        requiresAuth = true,
                        requiresIdempotencyKey = true,
                        implemented = false,
                        description = "Transferencia a terceros — endpoint pendiente en este backend",
                    ),
                )
            IntentLabel.VIEW_CHAT_HISTORY ->
                listOf(
                    BackendRouteHint(
                        method = "GET",
                        pathTemplate = "/v1/me/chat/history",
                        requiresAuth = true,
                        requiresIdempotencyKey = false,
                        implemented = true,
                        description = "Listar sesiones de chat del usuario con el asistente",
                    ),
                )
            IntentLabel.MONTHLY_EXPENSES ->
                listOf(
                    BackendRouteHint(
                        method = "GET",
                        pathTemplate = "/v1/me/expenses/by-category",
                        requiresAuth = true,
                        requiresIdempotencyKey = false,
                        implemented = true,
                        description = "Resumen de gastos por categoría en un mes (query yearMonth=YYYY-MM)",
                    ),
                )
            IntentLabel.AMBIGUOUS, IntentLabel.OUT_OF_SCOPE ->
                emptyList()
        }
    }
}
