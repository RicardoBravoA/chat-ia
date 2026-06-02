package com.bank.banking.domain.model

/**
 * Monto monetario (valor principal en [Double], ej. soles enteros o con decimales).
 */
data class Money(
    val amount: Double,
    val currency: String = "PEN",
) {
    init {
        require(!amount.isNaN() && !amount.isInfinite()) { "amount must be a finite number" }
        require(amount >= 0) { "amount must be non-negative" }
        require(currency.isNotBlank()) { "currency required" }
    }

    operator fun compareTo(other: Money): Int {
        require(currency == other.currency) { "currency mismatch" }
        return amount.compareTo(other.amount)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "currency mismatch" }
        return copy(amount = amount - other.amount)
    }
}
