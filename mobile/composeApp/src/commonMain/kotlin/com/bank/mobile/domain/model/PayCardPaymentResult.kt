package com.bank.mobile.domain.model

data class PayCardPaymentResult(
    val newAccountAmount: Double,
    val newCardDebt: Double,
    val currency: String,
    val movementId: String,
)
