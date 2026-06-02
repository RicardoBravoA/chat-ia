package com.bank.banking.application.usecase

import com.bank.banking.domain.model.Money
import com.bank.banking.domain.port.PayCreditCardResult

internal fun PayCreditCardResult.toPayload(): String =
    listOf(
        newAccountBalance.amount.toString(),
        newCardDebt.amount.toString(),
        newAccountBalance.currency,
        movementId,
    ).joinToString("|")

internal fun parsePayCreditCardPayload(raw: String): PayCreditCardResult {
    val parts = raw.split("|")
    require(parts.size == 4) { "invalid idempotency payload" }
    val bal = Money(parts[0].toDouble(), parts[2])
    val debt = Money(parts[1].toDouble(), parts[2])
    return PayCreditCardResult(bal, debt, parts[3])
}
