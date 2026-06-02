package com.bank.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponseDto(
    val amount: Double,
    val currency: String,
    val nickname: String = "",
)
