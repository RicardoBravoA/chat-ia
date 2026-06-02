package com.bank.banking.application.usecase

import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.error.InsufficientFundsException
import com.bank.banking.domain.error.InvalidPaymentAmountException
import com.bank.banking.domain.error.NotFoundException
import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.AccountRepository
import com.bank.banking.domain.port.CreditCardRepository
import com.bank.banking.domain.port.IdempotencyRepository
import com.bank.banking.domain.port.PayCreditCardResult
import com.bank.banking.domain.port.PaymentExecutionGateway
import com.bank.banking.domain.port.SecureTokenGenerator
import com.bank.banking.domain.port.SessionRepository

class PayCreditCardUseCase(
    private val sessions: SessionRepository,
    private val accounts: AccountRepository,
    private val cards: CreditCardRepository,
    private val idempotency: IdempotencyRepository,
    private val paymentGateway: PaymentExecutionGateway,
    private val tokens: SecureTokenGenerator,
) {
    suspend fun execute(
        token: SessionToken,
        cardId: CreditCardId,
        idempotencyKey: String,
        command: PayCreditCardCommand,
    ): PayCreditCardResult {
        val session = sessions.findValid(token) ?: throw UnauthorizedException()
        val userId = session.userId

        idempotency.getResult(idempotencyKey)?.let { return parsePayCreditCardPayload(it) }

        val reserved = idempotency.tryBegin(idempotencyKey, userId)
        if (!reserved) {
            val cached = idempotency.getResult(idempotencyKey)
                ?: throw IllegalStateException("Idempotency in progress; retry")
            return parsePayCreditCardPayload(cached)
        }

        try {
            val account = accounts.findPrimaryByUserId(userId) ?: throw NotFoundException("Account not found")
            val card = cards.findByIdAndUser(cardId, userId) ?: throw NotFoundException("Card not found")

            if (!cardholdersMatch(card.cardholderName, command.cardholderName)) {
                throw BadRequestException("Cardholder name does not match card on file")
            }
            verifyExpiryMatchesCard(card, command.expiryMonth, command.expiryYear)
            verifyCardNotExpired(command.expiryMonth, command.expiryYear)

            val paymentAmount = resolvePaymentAmount(card, command)

            if (paymentAmount.amount <= 0.0) {
                throw InvalidPaymentAmountException("Invalid payment amount")
            }
            if (paymentAmount.currency != account.balance.currency || paymentAmount.currency != card.debt.currency) {
                throw InvalidPaymentAmountException("Currency mismatch")
            }
            if (paymentAmount.amount > account.balance.amount) throw InsufficientFundsException()

            val movementId = tokens.newMovementId()
            val result = paymentGateway.payCreditCardFromAccount(
                userId = userId,
                cardId = cardId,
                amount = paymentAmount,
                movementId = movementId,
                correlationId = idempotencyKey,
                paymentMode = command.mode,
            )

            idempotency.complete(idempotencyKey, result.toPayload())
            return result
        } catch (e: Exception) {
            throw e
        }
    }
}
