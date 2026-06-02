package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.SessionRecord
import com.bank.banking.domain.port.SessionRepository
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant
import org.bson.Document

class MongoSessionRepository(
    private val db: MongoDatabase,
) : SessionRepository {
    private val col get() = db.getCollection<Document>(Collections.SESSIONS)

    suspend fun ensureIndexes() {
        col.createIndex(Indexes.ascending("expiresAt"))
    }

    override suspend fun create(userId: UserId, token: SessionToken, expiresAt: Instant) {
        col.insertOne(
            Document(
                mapOf(
                    "_id" to token.value,
                    "userId" to userId.value,
                    "expiresAt" to expiresAt.toEpochMilliseconds(),
                ),
            ),
        )
    }

    override suspend fun findValid(token: SessionToken): SessionRecord? {
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val doc = col.find(Filters.eq("_id", token.value)).limit(1).toList().firstOrNull() ?: return null
        val expMs = doc.getLong("expiresAt")
        if (expMs < now.toEpochMilliseconds()) return null
        return SessionRecord(
            token = token,
            userId = UserId(doc.getString("userId")),
            expiresAt = Instant.fromEpochMilliseconds(expMs),
        )
    }

    override suspend fun revoke(token: SessionToken) {
        col.deleteOne(Filters.eq("_id", token.value))
    }
}
