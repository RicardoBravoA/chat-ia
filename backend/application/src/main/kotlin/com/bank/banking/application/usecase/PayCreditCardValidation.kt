package com.bank.banking.application.usecase

import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.error.InvalidPaymentAmountException
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardPaymentMode
import com.bank.banking.domain.model.Money
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun normalizeCardholderName(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ")

internal fun cardholdersMatch(stored: String, provided: String): Boolean =
    normalizeCardholderName(stored).equals(normalizeCardholderName(provided), ignoreCase = true)

internal fun verifyCardNotExpired(expiryMonth: Int, expiryYear: Int) {
    if (expiryMonth !in 1..12) throw BadRequestException("expiryMonth must be 1–12")
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val y = now.year
    val m = now.monthNumber
    val stillValid = expiryYear > y || (expiryYear == y && expiryMonth >= m)
    if (!stillValid) {
        throw BadRequestException("Card is expired")
    }
}

internal fun verifyExpiryMatchesCard(card: CreditCard, expiryMonth: Int, expiryYear: Int) {
    if (card.expiryMonth != expiryMonth || card.expiryYear != expiryYear) {
        throw BadRequestException("Expiry does not match card on file")
    }
}

internal fun resolvePaymentAmount(card: CreditCard, command: PayCreditCardCommand): Money {
    val currency = card.debt.currency
    val debtAmt = card.debt.amount
    return when (command.mode) {
        CreditCardPaymentMode.MINIMUM -> {
            val v = kotlin.math.min(card.minimumPaymentDue.amount, debtAmt)
            if (v <= 0.0) {
                throw InvalidPaymentAmountException("No minimum payment due for this card")
            }
            Money(v, currency)
        }
        CreditCardPaymentMode.STATEMENT_MONTH -> {
            val v = kotlin.math.min(card.statementBalanceDue.amount, debtAmt)
            if (v <= 0.0) {
                throw InvalidPaymentAmountException("No statement balance due for this card")
            }
            Money(v, currency)
        }
        CreditCardPaymentMode.FULL_DEBT -> {
            if (debtAmt <= 0.0) throw InvalidPaymentAmountException("Card has no debt")
            Money(debtAmt, currency)
        }
        CreditCardPaymentMode.CUSTOM -> {
            val raw = command.customAmount
                ?: throw InvalidPaymentAmountException("amount required for CUSTOM paymentMode")
            if (raw <= 0.0 || raw > debtAmt) {
                throw InvalidPaymentAmountException("amount must be between 0 and total debt")
            }
            Money(raw, currency)
        }
    }
}
