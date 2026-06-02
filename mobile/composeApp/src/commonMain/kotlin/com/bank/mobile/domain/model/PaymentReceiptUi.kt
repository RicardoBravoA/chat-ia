package com.bank.mobile.domain.model

/**
 * Datos para mostrar el comprobante de pago de tarjeta en el chat y exportar imagen a la galería.
 */
data class PaymentReceiptUi(
    val amountPaid: Double,
    val currency: String,
    val movementId: String,
    val occurredAtEpochMs: Long,
)
