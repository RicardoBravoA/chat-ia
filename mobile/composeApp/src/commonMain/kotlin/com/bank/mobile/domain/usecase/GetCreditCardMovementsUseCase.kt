package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.model.Payment
import com.bank.mobile.domain.repository.HomeRepository

class GetCreditCardMovementsUseCase(private val repo: HomeRepository) {
    suspend operator fun invoke(token: String, cardId: String): List<Payment> =
        repo.getCreditCardMovements(token, cardId)
}
