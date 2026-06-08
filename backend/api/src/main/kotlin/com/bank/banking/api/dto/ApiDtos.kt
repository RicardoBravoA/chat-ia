package com.bank.banking.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val tokenType: String = "Bearer")

@Serializable
data class BalanceResponse(
    val amount: Double,
    val currency: String,
    val nickname: String = "",
)

@Serializable
data class CreditCardWithDebtItem(
    val cardId: String,
    val alias: String,
    val lastFourDigits: String,
    val cardholderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    /** Deuda total actual (misma escala que cuenta/saldo). */
    val debt: Double,
    /** Línea de crédito máxima. */
    val creditLine: Double,
    /** Pago mínimo del periodo. */
    val minimumPaymentDue: Double,
    /** Saldo al corte / pago del mes. */
    val statementBalanceDue: Double,
    val currency: String,
)

@Serializable
data class PayCardRequest(
    val cardholderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    /** [com.bank.banking.domain.model.CreditCardPaymentMode] serializado como string. */
    val paymentMode: String,
    /** Obligatorio si paymentMode es CUSTOM. */
    val amount: Double? = null,
)

@Serializable
data class MovementItem(
    val id: String,
    val cardId: String,
    val kind: String,
    /** Importe del movimiento. */
    val amount: Double,
    val currency: String,
    val description: String,
    val occurredAtEpochMs: Long,
)

@Serializable
data class PaymentItem(
    val id: String,
    val movementId: String,
    val cardId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val occurredAtEpochMs: Long,
)

@Serializable
data class PayCardResponse(
    val newAccountAmount: Double,
    val newCardDebt: Double,
    val currency: String,
    val movementId: String,
)

@Serializable
data class ErrorResponse(val code: String, val message: String)

@Serializable
data class ChatRouteRequest(val message: String)

@Serializable
data class SuggestedBackendActionDto(
    val method: String,
    val pathTemplate: String,
    val requiresAuth: Boolean,
    val requiresIdempotencyKey: Boolean,
    val implemented: Boolean,
    val description: String,
)

@Serializable
data class ChatRouteResponse(
    val userMessage: String,
    val intent: String,
    val confidence: Double,
    val clarificationNeeded: Boolean,
    val reason: String,
    val routerSource: String,
    val entities: Map<String, String>,
    val suggestedActions: List<SuggestedBackendActionDto>,
)

@Serializable
data class ChatMessageRequest(
    val message: String,
    val sessionId: String? = null,
    val selectedIntent: String? = null,
)

@Serializable
data class UiActionDto(
    val id: String,
    val label: String,
    val actionType: String,
    val payload: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
)

@Serializable
data class UiNodeDto(
    val id: String,
    val type: String,
    val props: Map<String, String> = emptyMap(),
    val children: List<UiNodeDto> = emptyList(),
    val actions: List<UiActionDto> = emptyList(),
)

@Serializable
data class UiMetadataDto(
    val intent: String,
    val confidence: Double,
    val clarificationNeeded: Boolean,
    val reason: String,
    val routerSource: String,
)

@Serializable
data class ChatMessageResponse(
    val schemaVersion: Int,
    val correlationId: String,
    val sessionId: String,
    val userMessage: String,
    val uiTree: UiNodeDto,
    val metadata: UiMetadataDto,
)

@Serializable
data class ChatWsEnvelope(
    val event: String,
    val response: ChatMessageResponse? = null,
    val error: ErrorResponse? = null,
)

@Serializable
data class ChatHistoryItemDto(
    val sessionId: String,
    val turnCount: Int,
    val lastUserMessage: String,
    val lastIntent: String?,
    val updatedAtEpochMs: Long,
)

@Serializable
data class ChatHistoryResponse(
    val sessions: List<ChatHistoryItemDto>,
)

@Serializable
data class ExpenseCategorySummaryDto(
    val categoryId: String,
    val category: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val currency: String,
)

@Serializable
data class MonthlyExpensesResponse(
    val yearMonth: String,
    val categories: List<ExpenseCategorySummaryDto>,
    val totalTransactionCount: Int,
    val grandTotal: Double,
    val currency: String,
)
