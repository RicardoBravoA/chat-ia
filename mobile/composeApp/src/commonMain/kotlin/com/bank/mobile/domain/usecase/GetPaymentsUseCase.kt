package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.model.Payment
import com.bank.mobile.domain.repository.PaymentsRepository

class GetPaymentsUseCase(private val repo: PaymentsRepository) {
    suspend operator fun invoke(token: String): List<Payment> = repo.getPayments(token)
}
