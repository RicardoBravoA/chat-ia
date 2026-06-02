package com.bank.mobile.domain.model

data class Balance(
    val amount: Double,
    val currency: String,
    val nickname: String = "",
)
