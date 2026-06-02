package com.bank.mobile.domain.model

/** Debe coincidir con `CreditCardPaymentMode` del backend. */
enum class CreditCardPaymentMode {
    MINIMUM,
    STATEMENT_MONTH,
    FULL_DEBT,
    CUSTOM,
}
