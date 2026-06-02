package com.bank.banking.domain.port

import com.bank.banking.domain.model.CardPayment
import com.bank.banking.domain.model.UserId

interface PaymentHistoryRepository {
    suspend fun listByUser(userId: UserId, limit: Int = 50): List<CardPayment>
}
