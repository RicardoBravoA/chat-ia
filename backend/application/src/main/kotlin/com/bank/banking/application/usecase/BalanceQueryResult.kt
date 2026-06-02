package com.bank.banking.application.usecase

import com.bank.banking.domain.model.Money

data class BalanceQueryResult(
    val balance: Money,
    val nickname: String,
)
