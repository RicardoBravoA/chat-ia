package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.ExpenseCategorySummary
import com.bank.banking.domain.model.MonthlyExpenseReport
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.ExpenseRepository
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Date
import kotlin.math.round

class MongoExpenseRepository(
    private val db: MongoDatabase,
) : ExpenseRepository {
    private val categories get() = db.getCollection<Document>(Collections.EXPENSE_CATEGORIES)
    private val expenses get() = db.getCollection<Document>(Collections.CATEGORIZED_EXPENSES)

    suspend fun ensureIndexes() {
        categories.createIndex(Indexes.ascending("code"), IndexOptions().unique(true))
        expenses.createIndex(Indexes.ascending("userId", "occurredAt"))
        expenses.createIndex(Indexes.ascending("userId", "categoryId", "occurredAt"))
    }

    override suspend fun summarizeByCategoryForMonth(userId: UserId, yearMonth: String): MonthlyExpenseReport {
        val month = YearMonth.parse(yearMonth)
        val rangeStartMs = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val rangeEndMs = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val rows = expenses.find(Filters.eq("userId", userId.value))
            .toList()
            .filter { doc ->
                val occurredAtMs = ExpenseDocumentSupport.readOccurredAtEpochMs(doc) ?: return@filter false
                occurredAtMs in rangeStartMs until rangeEndMs
            }

        val labels = categories.find().toList().associate { doc ->
            doc.getString("_id") to doc.getString("label")
        }

        val grouped = rows.groupBy { it.getString("categoryId") }
        val summaries = grouped.map { (categoryId, docs) ->
            val total = docs.sumOf { readAmount(it) }
            ExpenseCategorySummary(
                categoryId = categoryId,
                categoryLabel = labels[categoryId] ?: categoryId,
                transactionCount = docs.size,
                totalAmount = roundAmount(total),
                currency = docs.firstOrNull()?.getString("currency") ?: DEFAULT_CURRENCY,
            )
        }.sortedByDescending { it.totalAmount }

        val currency = summaries.firstOrNull()?.currency ?: DEFAULT_CURRENCY
        return MonthlyExpenseReport(
            yearMonth = yearMonth,
            categories = summaries,
            totalTransactionCount = rows.size,
            grandTotal = roundAmount(summaries.sumOf { it.totalAmount }),
            currency = currency,
        )
    }

    private fun readAmount(doc: Document): Double {
        val value = doc.get("amount") ?: return 0.0
        return when (value) {
            is Number -> value.toDouble()
            else -> 0.0
        }
    }

    private fun roundAmount(value: Double): Double = round(value * 100.0) / 100.0

    companion object {
        const val DEFAULT_CURRENCY = "PEN"
    }
}

internal object ExpenseDocumentSupport {
    fun readOccurredAtEpochMs(doc: Document): Long? {
        val value = doc.get("occurredAt") ?: return null
        return when (value) {
            is Date -> value.time
            is Number -> value.toLong()
            else -> null
        }
    }
}
