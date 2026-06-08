package com.bank.mobile.domain.repository

import com.bank.mobile.domain.model.Balance
import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardPaymentResult
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.domain.model.sdui.ChatUiResponse

interface HomeRepository {
    suspend fun getBalance(token: String): Balance
    suspend fun sendChatMessage(
        token: String,
        message: String,
        sessionId: String?,
        selectedIntent: String? = null,
    ): ChatUiResponse
    suspend fun getCreditCardMovements(token: String, cardId: String): List<Payment>
    suspend fun payCreditCard(
        token: String,
        cardId: String,
        cardholderName: String,
        expiryMonth: Int,
        expiryYear: Int,
        mode: CreditCardPaymentMode,
        customAmount: Double?,
    ): PayCardPaymentResult
}
