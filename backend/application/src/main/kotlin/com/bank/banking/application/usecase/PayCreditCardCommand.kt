package com.bank.banking.application.usecase

import com.bank.banking.domain.model.CreditCardPaymentMode

data class PayCreditCardCommand(
    val cardholderName: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val mode: CreditCardPaymentMode,
    /** Obligatorio si [mode] es [CreditCardPaymentMode.CUSTOM]. */
    val customAmount: Double? = null,
)
