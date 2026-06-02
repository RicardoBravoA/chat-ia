package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.CreditCardMovement
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.MovementKind
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.CreditCardRepository
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant
import org.bson.Document

class MongoCreditCardRepository(
    private val db: MongoDatabase,
) : CreditCardRepository {
    private val cards get() = db.getCollection<Document>(Collections.CREDIT_CARDS)
    private val movements get() = db.getCollection<Document>(Collections.CARD_MOVEMENTS)

    override suspend fun findByIdAndUser(cardId: CreditCardId, userId: UserId): CreditCard? {
        val doc = cards.find(
            Filters.and(
                Filters.eq("_id", cardId.value),
                Filters.eq("userId", userId.value),
            ),
        ).limit(1).toList().firstOrNull() ?: return null
        return doc.toCreditCardOrNull()
    }

    override suspend fun listWithDebtByUser(userId: UserId): List<CreditCard> {
        return cards
            .find(
                Filters.and(
                    Filters.eq("userId", userId.value),
                    Filters.gt("debt", 0.0),
                ),
            )
            .sort(Sorts.ascending("alias"))
            .toList()
            .mapNotNull { it.toCreditCardOrNull() }
            .filter { it.debt.amount > 0.0 }
    }

    override suspend fun listMovementsByUser(userId: UserId, limit: Int): List<CreditCardMovement> {
        val userCardIds = cards
            .find(Filters.eq("userId", userId.value))
            .toList()
            .mapNotNull { it.getString("_id") }
        if (userCardIds.isEmpty()) return emptyList()
        return movements
            .find(Filters.`in`("cardId", userCardIds))
            .sort(Sorts.descending("occurredAt"))
            .limit(limit)
            .toList()
            .map { doc ->
                val cardId = CreditCardId(doc.getString("cardId"))
                doc.toMovement(cardId)
            }
    }

    override suspend fun listMovements(cardId: CreditCardId, userId: UserId, limit: Int): List<CreditCardMovement> {
        val card = findByIdAndUser(cardId, userId) ?: return emptyList()
        return movements.find(Filters.eq("cardId", cardId.value))
            .sort(Sorts.descending("occurredAt"))
            .limit(limit)
            .toList()
            .map { it.toMovement(cardId) }
    }

    /**
     * Omite documentos inválidos en lugar de lanzar (evita 500 cuando hay BSON inconsistente
     * o campos nulos, p. ej. [currency] ausente).
     */
    private fun Document.toCreditCardOrNull(): CreditCard? {
        return try {
            val id = getString("_id") ?: return null
            val uid = getString("userId") ?: return null
            val alias = getString("alias") ?: return null
            val lastFour = getString("lastFourDigits") ?: return null
            val cur = getString("currency")?.trim()?.takeIf { it.isNotBlank() } ?: return null
            val debtAmt = getNumericDouble("debt") ?: return null
            val lineAmt = getNumericDouble("creditLine") ?: return null
            if (debtAmt < 0.0 || lineAmt < 0.0) return null
            val holder = getString("cardholderName")?.trim()?.takeIf { it.isNotBlank() } ?: alias
            val expM = getInt("expiryMonth", 12).coerceIn(1, 12)
            val expY = getInt("expiryYear", 2030)
            val minDue = getNumericDouble("minimumPaymentDue")
                ?: kotlin.math.max(0.0, kotlin.math.min(debtAmt * 0.05, debtAmt))
            val stmtDue = getNumericDouble("statementBalanceDue") ?: debtAmt
            if (minDue < 0.0 || stmtDue < 0.0) return null
            CreditCard(
                id = CreditCardId(id),
                userId = UserId(uid),
                alias = alias,
                lastFourDigits = lastFour,
                cardholderName = holder,
                expiryMonth = expM,
                expiryYear = expY,
                debt = Money(debtAmt, cur),
                creditLine = Money(lineAmt, cur),
                minimumPaymentDue = Money(minDue, cur),
                statementBalanceDue = Money(kotlin.math.min(stmtDue, debtAmt), cur),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun Document.toMovement(cardId: CreditCardId): CreditCardMovement =
        CreditCardMovement(
            id = getString("_id"),
            creditCardId = cardId,
            kind = getString("kind").toMovementKind(),
            amount = Money(getNumericDouble("amount") ?: error("amount"), getString("currency")),
            description = getString("description"),
            occurredAt = Instant.fromEpochMilliseconds(getLong("occurredAt")),
        )

    private fun String.toMovementKind(): MovementKind =
        when (uppercase()) {
            "CHARGE" -> MovementKind.CHARGE
            "PAID", "PAYMENT", "PAGADO" -> MovementKind.PAID
            else -> throw IllegalArgumentException("Unknown movement kind: $this")
        }
}
