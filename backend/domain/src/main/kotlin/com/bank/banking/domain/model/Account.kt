package com.bank.banking.domain.model

data class Account(
    val id: AccountId,
    val userId: UserId,
    val balance: Money,
    /** Perfil ligado a la cuenta (persistido en el documento `accounts` en MongoDB). */
    val nickname: String,
)
