package com.bank.mobile.domain.model

data class Payment(
    val id: String,
    val movementId: String,
    val cardId: String,
    val description: String = "",
    val amount: Double,
    val currency: String,
    val status: String,
    val occurredAtEpochMs: Long,
)
