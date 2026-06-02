package com.bank.mobile.domain.repository

import com.bank.mobile.domain.model.Payment

interface PaymentsRepository {
    suspend fun getPayments(token: String): List<Payment>
}
