package com.bank.banking.domain.model

import kotlinx.datetime.Instant

enum class MovementKind {
    CHARGE,
    PAID,
}

data class CreditCardMovement(
    val id: String,
    val creditCardId: CreditCardId,
    val kind: MovementKind,
    val amount: Money,
    val description: String,
    val occurredAt: Instant,
)
