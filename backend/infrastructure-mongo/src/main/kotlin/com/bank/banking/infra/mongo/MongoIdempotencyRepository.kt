package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.IdempotencyRepository
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.Document

class MongoIdempotencyRepository(
    private val db: MongoDatabase,
) : IdempotencyRepository {
    private val col get() = db.getCollection<Document>(Collections.IDEMPOTENCY)

    override suspend fun tryBegin(key: String, userId: UserId): Boolean {
        return try {
            col.insertOne(
                Document(
                    mapOf(
                        "_id" to key,
                        "userId" to userId.value,
                        "result" to null,
                        "createdAt" to System.currentTimeMillis(),
                    ),
                ),
            )
            true
        } catch (e: MongoWriteException) {
            if (e.code == 11000) false else throw e
        }
    }

    override suspend fun complete(key: String, resultSummary: String) {
        col.updateOne(
            Filters.eq("_id", key),
            Updates.set("result", resultSummary),
        )
    }

    override suspend fun getResult(key: String): String? {
        val doc = col.find(Filters.eq("_id", key)).limit(1).toList().firstOrNull() ?: return null
        return doc.getString("result")
    }
}
