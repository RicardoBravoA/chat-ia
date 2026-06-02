package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.CardPayment
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.PaymentHistoryRepository
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant
import org.bson.Document

class MongoPaymentHistoryRepository(
    private val db: MongoDatabase,
) : PaymentHistoryRepository {
    private val payments get() = db.getCollection<Document>(Collections.CARD_PAYMENTS)

    override suspend fun listByUser(userId: UserId, limit: Int): List<CardPayment> =
        payments.find(Filters.eq("userId", userId.value))
            .sort(Sorts.descending("occurredAt"))
            .limit(limit)
            .toList()
            .map { it.toPayment() }

    private fun Document.toPayment(): CardPayment =
        CardPayment(
            id = getString("_id"),
            movementId = getString("movementId"),
            cardId = CreditCardId(getString("cardId")),
            amount = Money(getNumericDouble("amount") ?: error("amount"), getString("currency")),
            status = getString("status"),
            occurredAt = Instant.fromEpochMilliseconds(getLong("occurredAt")),
        )
}
