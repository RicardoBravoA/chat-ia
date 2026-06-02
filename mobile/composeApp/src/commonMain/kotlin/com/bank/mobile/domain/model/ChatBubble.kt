package com.bank.mobile.domain.model

import com.bank.mobile.domain.model.sdui.UiNode

data class PayCardChatAction(
    val cardId: String,
    val alias: String,
    val lastFourDigits: String,
    val cardholderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val debt: Double,
    val creditLine: Double,
    val minimumPaymentDue: Double,
    val statementBalanceDue: Double,
    val currency: String,
)

data class ChatBubble(
    val text: String,
    val isUser: Boolean,
    /** Respuesta del asistente vía WS /v1/chat/ws (SDUI). */
    val sduiRoot: UiNode? = null,
    /** Tarjetas ya pagadas en este mensaje SDUI (evita doble pago en el mismo panel). */
    val paidCardIds: Set<String> = emptySet(),
    val paymentReceipt: PaymentReceiptUi? = null,
    val timestampEpochMs: Long,
)
