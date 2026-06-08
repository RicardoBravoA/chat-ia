package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.ChatHistoryEntry
import com.bank.banking.domain.model.ChatSession
import com.bank.banking.domain.model.ChatSessionId
import com.bank.banking.domain.model.ChatTurn
import com.bank.banking.domain.model.UserId
import com.mongodb.client.model.Sorts
import com.bank.banking.domain.port.ChatSessionRepository
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.PushOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import java.util.UUID

class MongoChatSessionRepository(
    private val db: MongoDatabase,
) : ChatSessionRepository {
    private val col get() = db.getCollection<Document>(Collections.CHAT_SESSIONS)

    suspend fun ensureIndexes() {
        col.createIndex(Indexes.ascending("userId", "updatedAtEpochMs"))
    }

    override suspend fun create(userId: UserId): ChatSessionId {
        val id = ChatSessionId(UUID.randomUUID().toString())
        val now = System.currentTimeMillis()
        col.insertOne(
            Document(
                mapOf(
                    "_id" to id.value,
                    "userId" to userId.value,
                    "turns" to emptyList<Document>(),
                    "updatedAtEpochMs" to now,
                ),
            ),
        )
        return id
    }

    override suspend fun findForUser(sessionId: ChatSessionId, userId: UserId): ChatSession? {
        val doc = col.find(
            Filters.and(
                Filters.eq("_id", sessionId.value),
                Filters.eq("userId", userId.value),
            ),
        ).limit(1).toList().firstOrNull() ?: return null
        return doc.toDomain()
    }

    override suspend fun listRecentTurns(sessionId: ChatSessionId, userId: UserId, limit: Int): List<ChatTurn> {
        val session = findForUser(sessionId, userId) ?: return emptyList()
        return session.turns.takeLast(limit.coerceAtLeast(0))
    }

    override suspend fun listSessionsForUser(userId: UserId, limit: Int): List<ChatHistoryEntry> {
        val capped = limit.coerceIn(1, MAX_LIST_SESSIONS)
        return col.find(Filters.eq("userId", userId.value))
            .sort(Sorts.descending("updatedAtEpochMs"))
            .limit(capped)
            .toList()
            .map { it.toHistoryEntry() }
    }

    override suspend fun appendTurn(sessionId: ChatSessionId, userId: UserId, turn: ChatTurn) {
        val result = col.updateOne(
            Filters.and(
                Filters.eq("_id", sessionId.value),
                Filters.eq("userId", userId.value),
            ),
            Updates.combine(
                Updates.pushEach(
                    "turns",
                    listOf(turn.toDocument()),
                    PushOptions().slice(-MAX_STORED_TURNS),
                ),
                Updates.set("updatedAtEpochMs", turn.createdAtEpochMs),
            ),
        )
        check(result.matchedCount == 1L) { "chat session not found" }
    }

    private fun Document.toDomain(): ChatSession {
        val turnsDocs = getList("turns", Document::class.java) ?: emptyList()
        return ChatSession(
            id = ChatSessionId(getString("_id")),
            userId = UserId(getString("userId")),
            turns = turnsDocs.map { it.toTurn() },
            updatedAtEpochMs = getLong("updatedAtEpochMs"),
        )
    }

    private fun Document.toTurn(): ChatTurn {
        val entitiesDoc = get("entities", Document::class.java)
        val entities = entitiesDoc?.entries
            ?.associate { (key, value) -> key.toString() to value.toString() }
            ?: emptyMap()
        return ChatTurn(
            userMessage = getString("userMessage"),
            intent = getString("intent"),
            correlationId = getString("correlationId"),
            createdAtEpochMs = getLong("createdAtEpochMs"),
            confidence = readOptionalDouble("confidence"),
            reason = getString("reason"),
            routerSource = getString("routerSource"),
            entities = entities,
        )
    }

    private fun Document.readOptionalDouble(field: String): Double? {
        val value = get(field) ?: return null
        return when (value) {
            is Number -> value.toDouble()
            else -> null
        }
    }

    private fun ChatTurn.toDocument(): Document {
        val doc = Document(
            mapOf(
                "userMessage" to userMessage,
                "intent" to intent,
                "correlationId" to correlationId,
                "createdAtEpochMs" to createdAtEpochMs,
            ),
        )
        confidence?.let { doc["confidence"] = it }
        reason?.let { doc["reason"] = it }
        routerSource?.let { doc["routerSource"] = it }
        if (entities.isNotEmpty()) {
            doc["entities"] = Document(entities)
        }
        return doc
    }

    private fun Document.toHistoryEntry(): ChatHistoryEntry {
        val turnsDocs = getList("turns", Document::class.java) ?: emptyList()
        val lastTurn = turnsDocs.lastOrNull()
        return ChatHistoryEntry(
            sessionId = getString("_id"),
            turnCount = turnsDocs.size,
            lastUserMessage = lastTurn?.getString("userMessage") ?: "",
            lastIntent = lastTurn?.getString("intent"),
            updatedAtEpochMs = getLong("updatedAtEpochMs"),
        )
    }

    companion object {
        const val MAX_STORED_TURNS = 50
        const val MAX_LIST_SESSIONS = 20
    }
}
