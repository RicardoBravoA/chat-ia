package com.bank.mobile.data.mapper

import com.bank.mobile.data.remote.dto.BalanceResponseDto
import com.bank.mobile.data.remote.dto.MovementItemDto
import com.bank.mobile.data.remote.dto.PayCardResponseDto
import com.bank.mobile.domain.model.Balance
import com.bank.mobile.domain.model.PayCardPaymentResult
import com.bank.mobile.domain.model.Payment

internal fun BalanceResponseDto.toDomain(): Balance =
    Balance(amount = amount, currency = currency, nickname = nickname)

internal fun MovementItemDto.toPayment(): Payment =
    Payment(
        id = id,
        movementId = id,
        cardId = cardId,
        description = description,
        amount = amount,
        currency = currency,
        status = kind,
        occurredAtEpochMs = occurredAtMs,
    )

internal fun PayCardResponseDto.toDomain(): PayCardPaymentResult =
    PayCardPaymentResult(
        newAccountAmount = newAccountAmount,
        newCardDebt = newCardDebt,
        currency = currency,
        movementId = movementId,
    )
