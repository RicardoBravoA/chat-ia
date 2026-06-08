package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.ListChatHistoryUseCase
import com.bank.banking.domain.model.ChatHistoryEntry
import com.bank.banking.domain.model.ChatSessionId
import com.bank.banking.domain.model.ChatTurn
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.model.sdui.UiComponentType
import com.bank.banking.domain.port.ChatSessionRepository
import com.bank.banking.domain.port.SessionRecord
import com.bank.banking.domain.port.SessionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatHistoryUiBuilderTest {
    @Test
    fun `builds history rows when sessions exist`() = runBlocking {
        val token = SessionToken("tok")
        val userId = UserId("u1")
        val sessions = object : SessionRepository {
            override suspend fun create(userId: UserId, token: SessionToken, expiresAt: Instant) = Unit
            override suspend fun findValid(token: SessionToken) =
                SessionRecord(token, userId, Instant.fromEpochMilliseconds(Long.MAX_VALUE))
            override suspend fun revoke(token: SessionToken) = Unit
        }
        val chatSessions = object : ChatSessionRepository {
            override suspend fun create(userId: UserId) = ChatSessionId("s1")
            override suspend fun findForUser(sessionId: ChatSessionId, userId: UserId) = null
            override suspend fun listRecentTurns(sessionId: ChatSessionId, userId: UserId, limit: Int) = emptyList<ChatTurn>()
            override suspend fun listSessionsForUser(userId: UserId, limit: Int): List<ChatHistoryEntry> =
                listOf(
                    ChatHistoryEntry(
                        sessionId = "sess-1",
                        turnCount = 2,
                        lastUserMessage = "ver saldo",
                        lastIntent = "CHECK_BALANCE",
                        updatedAtEpochMs = 1_700_000_000_000L,
                    ),
                )
            override suspend fun appendTurn(sessionId: ChatSessionId, userId: UserId, turn: ChatTurn) = Unit
        }
        val tree = ChatHistoryUiBuilder(ListChatHistoryUseCase(sessions, chatSessions)).build(
            ChatUiBuildContext(
                token = token,
                userMessage = "historial",
                classification = IntentClassification(
                    intent = IntentLabel.VIEW_CHAT_HISTORY,
                    confidence = 0.95,
                    entities = emptyMap(),
                    clarificationNeeded = false,
                    reason = "test",
                    source = "test",
                ),
            ),
        )
        assertEquals(UiComponentType.COLUMN, tree.type)
        assertTrue(tree.children.any { it.type == UiComponentType.CHAT_HISTORY_ROW })
    }
}
