package com.bank.banking.infra.mongo

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Date

/**
 * Índices y datos demo (solo si colección users vacía).
 * Usuario: woz@bank.com / Demo1234!
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

        MongoExpenseRepository(db).ensureIndexes()
        seedExpenseDemoIfNeeded()

        val users = db.getCollection<Document>(Collections.USERS)
        if (users.find().limit(1).toList().isNotEmpty()) return

        val hash = passwordHasher.hash("Demo1234!")
        users.insertOne(
            Document(
                mapOf(
                    "_id" to "usr_demo_1",
                    "email" to "woz@bank.com",
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

    private suspend fun seedExpenseDemoIfNeeded() {
        val categories = db.getCollection<Document>(Collections.EXPENSE_CATEGORIES)
        if (categories.countDocuments() == 0L) {
            categories.insertMany(
                listOf(
                    Document(mapOf("_id" to "cat_food", "code" to "FOOD", "label" to "Alimentación")),
                    Document(mapOf("_id" to "cat_transport", "code" to "TRANSPORT", "label" to "Transporte")),
                    Document(mapOf("_id" to "cat_shopping", "code" to "SHOPPING", "label" to "Compras")),
                    Document(mapOf("_id" to "cat_bills", "code" to "BILLS", "label" to "Servicios")),
                    Document(mapOf("_id" to "cat_entertainment", "code" to "ENTERTAINMENT", "label" to "Ocio")),
                ),
            )
        }

        val expenses = db.getCollection<Document>(Collections.CATEGORIZED_EXPENSES)
        if (expenses.countDocuments(Document("userId", DEMO_USER_ID)) > 0L) return

        val month = YearMonth.now()
        fun day(dayOfMonth: Int): Date {
            val instant = month.atDay(dayOfMonth).atStartOfDay(ZoneOffset.UTC).plusHours(12).toInstant()
            return Date.from(instant)
        }

        expenses.insertMany(
            listOf(
                expenseDoc("exp_food_1", "cat_food", 45.50, "Almuerzo", day(1)),
                expenseDoc("exp_food_2", "cat_food", 32.00, "Cena", day(3)),
                expenseDoc("exp_food_3", "cat_food", 18.75, "Café", day(10)),
                expenseDoc("exp_trans_1", "cat_transport", 12.50, "Taxi", day(2)),
                expenseDoc("exp_trans_2", "cat_transport", 8.00, "Metro", day(8)),
                expenseDoc("exp_shop_1", "cat_shopping", 250.00, "Ropa", day(5)),
                expenseDoc("exp_shop_2", "cat_shopping", 89.90, "Supermercado", day(12)),
                expenseDoc("exp_bills_1", "cat_bills", 120.00, "Luz", day(15)),
                expenseDoc("exp_ent_1", "cat_entertainment", 35.00, "Cine", day(18)),
                expenseDoc("exp_ent_2", "cat_entertainment", 60.00, "Concierto", day(22)),
            ),
        )
    }

    private fun expenseDoc(
        id: String,
        categoryId: String,
        amount: Double,
        description: String,
        occurredAt: Date,
    ): Document =
        Document(
            mapOf(
                "_id" to id,
                "userId" to DEMO_USER_ID,
                "categoryId" to categoryId,
                "amount" to amount,
                "currency" to "PEN",
                "description" to description,
                "occurredAt" to occurredAt,
            ),
        )

    companion object {
        private const val DEMO_USER_ID = "usr_demo_1"
    }
}
