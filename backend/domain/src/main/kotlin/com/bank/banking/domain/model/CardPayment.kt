package com.bank.banking.domain.model

import kotlinx.datetime.Instant

data class CardPayment(
    val id: String,
    val movementId: String,
    val cardId: CreditCardId,
    val amount: Money,
    val status: String,
    val occurredAt: Instant,
)
