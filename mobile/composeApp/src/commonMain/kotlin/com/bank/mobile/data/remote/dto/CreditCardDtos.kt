package com.bank.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PayCardRequestDto(
    val cardholderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val paymentMode: String,
    val amount: Double? = null,
)

@Serializable
data class PayCardResponseDto(
    val newAccountAmount: Double,
    val newCardDebt: Double,
    val currency: String,
    val movementId: String,
)
