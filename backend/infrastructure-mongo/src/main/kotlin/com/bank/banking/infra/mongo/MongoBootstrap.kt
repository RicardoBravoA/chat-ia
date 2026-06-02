package com.bank.banking.infra.mongo

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document

/**
 * Índices y datos demo (solo si colección users vacía).
 * Usuario: demo@bank.com / Demo1234!
 */
class MongoBootstrap(
    private val db: MongoDatabase,
    private val passwordHasher: BcryptPasswordHasher,
) {
    suspend fun run() {
        val sessions = db.getCollection<Document>(Collections.SESSIONS)
        sessions.createIndex(Indexes.ascending("expiresAt"))

        val idem = db.getCollection<Document>(Collections.IDEMPOTENCY)
        idem.createIndex(Indexes.ascending("userId"), IndexOptions().unique(false))

        val payments = db.getCollection<Document>(Collections.CARD_PAYMENTS)
        payments.createIndex(Indexes.ascending("userId"), IndexOptions().unique(false))
        payments.createIndex(Indexes.ascending("cardId"), IndexOptions().unique(false))
        payments.createIndex(Indexes.ascending("occurredAt"), IndexOptions().unique(false))

        val users = db.getCollection<Document>(Collections.USERS)
        if (users.find().limit(1).toList().isNotEmpty()) return

        val hash = passwordHasher.hash("Demo1234!")
        users.insertOne(
            Document(
                mapOf(
                    "_id" to "usr_demo_1",
                    "email" to "demo@bank.com",
                    "passwordHash" to hash,
                ),
            ),
        )
        db.getCollection<Document>(Collections.ACCOUNTS).insertOne(
            Document(
                mapOf(
                    "_id" to "acc_demo_1",
                    "userId" to "usr_demo_1",
                    "amount" to 20_000.0,
                    "currency" to "PEN",
                    "nickname" to "Demo",
                ),
            ),
        )
        db.getCollection<Document>(Collections.CREDIT_CARDS).insertOne(
            Document(
                mapOf(
                    "_id" to "card_demo_1",
                    "userId" to "usr_demo_1",
                    "alias" to "oro",
                    "lastFourDigits" to "4242",
                    "cardholderName" to "Demo Usuario",
                    "expiryMonth" to 12,
                    "expiryYear" to 2028,
                    "debt" to 120_000.0,
                    "creditLine" to 300_000.0,
                    "minimumPaymentDue" to 6_000.0,
                    "statementBalanceDue" to 45_000.0,
                    "currency" to "PEN",
                ),
            ),
        )
        val now = System.currentTimeMillis()
        db.getCollection<Document>(Collections.CARD_MOVEMENTS).insertOne(
            Document(
                mapOf(
                    "_id" to "mov_seed_1",
                    "userId" to "usr_demo_1",
                    "cardId" to "card_demo_1",
                    "kind" to "CHARGE",
                    "amount" to 50_000.0,
                    "currency" to "PEN",
                    "description" to "Compra ejemplo",
                    "occurredAt" to (now - 86_400_000L),
                ),
            ),
        )
    }
}
