package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardPaymentResult
import com.bank.mobile.domain.repository.HomeRepository

class PayCreditCardUseCase(private val repo: HomeRepository) {
    suspend operator fun invoke(
        token: String,
        cardId: String,
        cardholderName: String,
        expiryMonth: Int,
        expiryYear: Int,
        mode: CreditCardPaymentMode,
        customAmount: Double?,
    ): PayCardPaymentResult = repo.payCreditCard(
        token = token,
        cardId = cardId,
        cardholderName = cardholderName,
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        mode = mode,
        customAmount = customAmount,
    )
}
