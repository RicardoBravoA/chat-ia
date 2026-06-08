package com.bank.banking.domain.port

import com.bank.banking.domain.model.ChatHistoryEntry
import com.bank.banking.domain.model.ChatSession
import com.bank.banking.domain.model.ChatSessionId
import com.bank.banking.domain.model.ChatTurn
import com.bank.banking.domain.model.UserId

interface ChatSessionRepository {
    suspend fun create(userId: UserId): ChatSessionId

    suspend fun findForUser(sessionId: ChatSessionId, userId: UserId): ChatSession?

    suspend fun listRecentTurns(sessionId: ChatSessionId, userId: UserId, limit: Int): List<ChatTurn>

    suspend fun listSessionsForUser(userId: UserId, limit: Int): List<ChatHistoryEntry>

    suspend fun appendTurn(sessionId: ChatSessionId, userId: UserId, turn: ChatTurn)
}
