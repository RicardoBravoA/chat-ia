package com.bank.banking.domain.model

data class CreditCard(
    val id: CreditCardId,
    val userId: UserId,
    val alias: String,
    val lastFourDigits: String,
    /** Nombre impreso / titular para verificación en pagos (no es secreto; la tarjeta ya está tokenizada por id). */
    val cardholderName: String,
    /** Mes de vencimiento (1–12). */
    val expiryMonth: Int,
    /** Año de vencimiento (p. ej. 2028). */
    val expiryYear: Int,
    /** Deuda total actual (suma pendiente) en unidades mínimas. */
    val debt: Money,
    /** Línea de crédito: máximo endeudamiento permitido (misma moneda que [debt]). */
    val creditLine: Money,
    /** Pago mínimo del periodo (misma moneda que [debt]). */
    val minimumPaymentDue: Money,
    /** Saldo al corte / “pago del mes” según estado de cuenta (misma moneda que [debt]). */
    val statementBalanceDue: Money,
)
