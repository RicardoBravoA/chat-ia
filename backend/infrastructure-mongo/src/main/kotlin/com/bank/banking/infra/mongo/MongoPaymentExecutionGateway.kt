package com.bank.banking.infra.mongo

import com.bank.banking.domain.error.InsufficientFundsException
import com.bank.banking.domain.error.InvalidPaymentAmountException
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.CreditCardPaymentMode
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.MovementKind
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.PayCreditCardResult
import com.bank.banking.domain.port.PaymentExecutionGateway
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.bson.Document

/**
 * Pago no atómico multi-documento: aplica débito en cuenta, luego en tarjeta, con rollback best-effort del débito
 * si la segunda fase falla. En producción preferir replica set + transacciones MongoDB.
 */
class MongoPaymentExecutionGateway(
    private val db: MongoDatabase,
) : PaymentExecutionGateway {
    private val accounts get() = db.getCollection<Document>(Collections.ACCOUNTS)
    private val cards get() = db.getCollection<Document>(Collections.CREDIT_CARDS)
    private val movements get() = db.getCollection<Document>(Collections.CARD_MOVEMENTS)
    private val payments get() = db.getCollection<Document>(Collections.CARD_PAYMENTS)

    override suspend fun payCreditCardFromAccount(
        userId: UserId,
        cardId: CreditCardId,
        amount: Money,
        movementId: String,
        correlationId: String,
        paymentMode: CreditCardPaymentMode,
    ): PayCreditCardResult {
        val accFilter = Filters.and(
            Filters.eq("userId", userId.value),
            Filters.gte("amount", amount.amount),
        )
        val accUpdate = accounts.updateOne(
            accFilter,
            Updates.inc("amount", -amount.amount),
        )
        if (accUpdate.modifiedCount == 0L) throw InsufficientFundsException()

        val cardFilter = Filters.and(
            Filters.eq("_id", cardId.value),
            Filters.eq("userId", userId.value),
            Filters.gte("debt", amount.amount),
        )
        val cardUpdate = cards.updateOne(
            cardFilter,
            Updates.inc("debt", -amount.amount),
        )
        if (cardUpdate.modifiedCount == 0L) {
            accounts.updateOne(
                Filters.eq("userId", userId.value),
                Updates.inc("amount", amount.amount),
            )
            throw InvalidPaymentAmountException("payment could not be applied to card")
        }

        val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        movements.insertOne(
            Document(
                mapOf(
                    "_id" to movementId,
                    "userId" to userId.value,
                    "cardId" to cardId.value,
                    "kind" to MovementKind.PAID.name,
                    "amount" to amount.amount,
                    "currency" to amount.currency,
                    "description" to "Pago tarjeta mode=${paymentMode.name} (correlation=$correlationId)",
                    "occurredAt" to now.toEpochMilliseconds(),
                ),
            ),
        )
        payments.insertOne(
            Document(
                mapOf(
                    "_id" to "pay_$movementId",
                    "movementId" to movementId,
                    "userId" to userId.value,
                    "cardId" to cardId.value,
                    "status" to "PAID",
                    "amount" to amount.amount,
                    "currency" to amount.currency,
                    "correlationId" to correlationId,
                    "paymentMode" to paymentMode.name,
                    "occurredAt" to now.toEpochMilliseconds(),
                ),
            ),
        )

        val accDoc = accounts.find(Filters.eq("userId", userId.value)).limit(1).toList().firstOrNull()
            ?: throw IllegalStateException("account missing after payment")
        val cardDoc = cards.find(Filters.eq("_id", cardId.value)).limit(1).toList().firstOrNull()
            ?: throw IllegalStateException("card missing after payment")

        return PayCreditCardResult(
            newAccountBalance = Money(accDoc.getNumericDouble("amount") ?: error("amount"), accDoc.getString("currency")),
            newCardDebt = Money(cardDoc.getNumericDouble("debt") ?: error("debt"), cardDoc.getString("currency")),
            movementId = movementId,
        )
    }
}
