package com.bank.banking.api

import com.bank.banking.api.dto.ChatRouteResponse
import com.bank.banking.api.dto.CreditCardWithDebtItem
import com.bank.banking.api.dto.MovementItem
import com.bank.banking.api.dto.PaymentItem
import com.bank.banking.api.dto.SuggestedBackendActionDto
import com.bank.banking.application.usecase.ChatRouteResult
import com.bank.banking.domain.model.BackendRouteHint
import com.bank.banking.domain.model.CardPayment
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardMovement

internal fun CreditCard.toCreditCardWithDebtItem(): CreditCardWithDebtItem =
    CreditCardWithDebtItem(
        cardId = id.value,
        alias = alias,
        lastFourDigits = lastFourDigits,
        cardholderName = cardholderName,
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        debt = debt.amount,
        creditLine = creditLine.amount,
        minimumPaymentDue = minimumPaymentDue.amount,
        statementBalanceDue = statementBalanceDue.amount,
        currency = debt.currency,
    )

internal fun CreditCardMovement.toMovementItem(): MovementItem =
    MovementItem(
        id = id,
        cardId = creditCardId.value,
        kind = kind.name,
        amount = amount.amount,
        currency = amount.currency,
        description = description,
        occurredAtEpochMs = occurredAt.toEpochMilliseconds(),
    )

internal fun CardPayment.toPaymentItem(): PaymentItem =
    PaymentItem(
        id = id,
        movementId = movementId,
        cardId = cardId.value,
        amount = amount.amount,
        currency = amount.currency,
        status = status,
        occurredAtEpochMs = occurredAt.toEpochMilliseconds(),
    )

internal fun BackendRouteHint.toDto(): SuggestedBackendActionDto =
    SuggestedBackendActionDto(
        method = method,
        pathTemplate = pathTemplate,
        requiresAuth = requiresAuth,
        requiresIdempotencyKey = requiresIdempotencyKey,
        implemented = implemented,
        description = description,
    )

internal fun ChatRouteResult.toResponse(): ChatRouteResponse {
    val c = classification
    return ChatRouteResponse(
        userMessage = userMessage,
        intent = c.intent.name,
        confidence = c.confidence,
        clarificationNeeded = c.clarificationNeeded,
        reason = c.reason,
        routerSource = c.source,
        entities = c.entities,
        suggestedActions = suggestedActions.map { it.toDto() },
    )
}
