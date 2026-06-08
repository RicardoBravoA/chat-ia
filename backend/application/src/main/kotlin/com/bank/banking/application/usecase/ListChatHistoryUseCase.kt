package com.bank.banking.application.usecase

import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.ChatHistoryEntry
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.ChatSessionRepository
import com.bank.banking.domain.port.SessionRepository

class ListChatHistoryUseCase(
    private val sessions: SessionRepository,
    private val chatSessions: ChatSessionRepository,
) {
    suspend fun execute(token: SessionToken, limit: Int = DEFAULT_LIMIT): List<ChatHistoryEntry> {
        val userId = sessions.findValid(token)?.userId
            ?: throw UnauthorizedException("Invalid or expired session")
        return chatSessions.listSessionsForUser(userId, limit)
    }

    companion object {
        const val DEFAULT_LIMIT = 10
    }
}
