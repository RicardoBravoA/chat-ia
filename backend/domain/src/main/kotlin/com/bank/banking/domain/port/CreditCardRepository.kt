package com.bank.banking.domain.port

import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.CreditCardMovement
import com.bank.banking.domain.model.CreditCardPaymentMode
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.UserId

interface CreditCardRepository {
    suspend fun findByIdAndUser(cardId: CreditCardId, userId: UserId): CreditCard?

    /**
     * Tarjetas del usuario con saldo pendiente estrictamente mayor a cero (solo deuda).
     */
    suspend fun listWithDebtByUser(userId: UserId): List<CreditCard>

    suspend fun listMovementsByUser(userId: UserId, limit: Int): List<CreditCardMovement>

    suspend fun listMovements(cardId: CreditCardId, userId: UserId, limit: Int): List<CreditCardMovement>
}

data class PayCreditCardResult(
    val newAccountBalance: Money,
    val newCardDebt: Money,
    val movementId: String,
)

/**
 * Ejecución atómica (idealmente transacción MongoDB) del pago desde cuenta corriente hacia la tarjeta.
 */
interface PaymentExecutionGateway {
    suspend fun payCreditCardFromAccount(
        userId: UserId,
        cardId: CreditCardId,
        amount: Money,
        movementId: String,
        correlationId: String,
        paymentMode: CreditCardPaymentMode,
    ): PayCreditCardResult
}
