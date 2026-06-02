package com.bank.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovementItemDto(
    val id: String,
    val cardId: String,
    val kind: String,
    val amount: Double,
    val currency: String,
    val description: String,
    @SerialName("occurredAtEpochMs") val occurredAtMs: Long,
)

@Serializable
data class PaymentItemDto(
    val id: String,
    val movementId: String,
    val cardId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    @SerialName("occurredAtEpochMs") val occurredAtMs: Long,
)
