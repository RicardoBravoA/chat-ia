package com.bank.banking.application.usecase

import com.bank.banking.application.sdui.BalanceUiBuilder
import com.bank.banking.application.sdui.ChatHistoryUiBuilder
import com.bank.banking.application.sdui.ChatUiBuilderFactory
import com.bank.banking.application.sdui.ClarificationUiBuilder
import com.bank.banking.application.sdui.GreetingUiBuilder
import com.bank.banking.application.sdui.MonthlyExpensesUiBuilder
import com.bank.banking.application.sdui.NotImplementedTransferUiBuilder
import com.bank.banking.application.sdui.PayCreditCardUiBuilder
import com.bank.banking.application.sdui.SupportUiBuilder
import com.bank.banking.domain.model.MonthlyExpenseReport
import com.bank.banking.domain.port.ExpenseRepository
import com.bank.banking.domain.model.Account
import com.bank.banking.domain.model.AccountId
import com.bank.banking.domain.model.ChatHistoryMessage
import com.bank.banking.domain.model.ChatHistoryRole
import com.bank.banking.domain.model.ChatSession
import com.bank.banking.domain.model.ChatSessionId
import com.bank.banking.domain.model.ChatTurn
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.CreditCardMovement
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.AccountRepository
import com.bank.banking.domain.port.ChatSessionRepository
import com.bank.banking.domain.port.CreditCardRepository
import com.bank.banking.domain.port.IntentClassifierPort
import com.bank.banking.domain.port.SessionRecord
import com.bank.banking.domain.port.SessionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

class BuildChatUiUseCaseSessionTest {
    @Test
    fun `quick reply uses selectedIntent without calling classifier`() = runBlocking {
        val ctx = TestContext()
        val token = SessionToken("tok")
        ctx.useCase.execute(
            token = token,
            message = "Pagar tarjeta",
            sessionId = null,
            selectedIntent = "PAY_CREDIT_CARD",
        )

        assertEquals(0, ctx.classifier.historyBatches.size)
    }

    @Test
    fun `creates session and returns sessionId on first message`() = runBlocking {
        val ctx = TestContext()
        val token = SessionToken("tok")

        val first = ctx.useCase.execute(token, "cuanto debo", sessionId = null)

        assertNotNull(first.sessionId)
        assertEquals(1, ctx.chatSessions.turnCount(ChatSessionId(first.sessionId)))
    }

    @Test
    fun `reuses session and passes prior turns to classifier`() = runBlocking {
        val ctx = TestContext()
        val token = SessionToken("tok")
        val first = ctx.useCase.execute(token, "cuanto debo en la oro", sessionId = null)
        val sessionId = ChatSessionId(first.sessionId)

        ctx.useCase.execute(token, "paga 50", sessionId)

        assertEquals(2, ctx.chatSessions.turnCount(sessionId))
        assertEquals(2, ctx.classifier.historyBatches.size)
        assertEquals(2, ctx.classifier.historyBatches[1].size)
        assertEquals(ChatHistoryRole.USER, ctx.classifier.historyBatches[1][0].role)
        assertEquals("cuanto debo en la oro", ctx.classifier.historyBatches[1][0].content)
        assertTrue(ctx.classifier.historyBatches[1][1].content.contains("\"cardAlias\":\"oro\""))
        val storedTurn = ctx.chatSessions.lastTurn(sessionId)
        assertEquals("oro", storedTurn?.entities?.get("cardAlias"))
    }

    @Test
    fun `stores classification metadata in session turn`() = runBlocking {
        val ctx = TestContext()
        val token = SessionToken("tok")
        val response = ctx.useCase.execute(token, "cuanto debo en la oro", sessionId = null)
        val turn = ctx.chatSessions.lastTurn(ChatSessionId(response.sessionId))

        assertEquals("CHECK_BALANCE", turn?.intent)
        assertEquals(0.9, turn?.confidence)
        assertEquals("oro", turn?.entities?.get("cardAlias"))
        assertEquals("woz", turn?.routerSource)
    }

    private class RecordingClassifier : IntentClassifierPort {
        val historyBatches = mutableListOf<List<ChatHistoryMessage>>()

        override suspend fun classify(message: String, history: List<ChatHistoryMessage>): IntentClassification {
            historyBatches.add(history)
            return if (message.contains("paga", ignoreCase = true)) {
                IntentClassification(
                    intent = IntentLabel.PAY_CREDIT_CARD,
                    confidence = 0.88,
                    entities = mapOf("amount" to "50", "cardAlias" to "oro"),
                    clarificationNeeded = false,
                    reason = "pago parcial",
                    source = "woz",
                )
            } else {
                IntentClassification(
                    intent = IntentLabel.CHECK_BALANCE,
                    confidence = 0.9,
                    entities = mapOf("cardAlias" to "oro"),
                    clarificationNeeded = false,
                    reason = "consulta deuda",
                    source = "woz",
                )
            }
        }
    }

    private class InMemoryChatSessions : ChatSessionRepository {
        private val sessions = ConcurrentHashMap<String, ChatSession>()

        override suspend fun create(userId: UserId): ChatSessionId {
            val id = ChatSessionId("sess-${sessions.size + 1}")
            sessions[id.value] = ChatSession(
                id = id,
                userId = userId,
                turns = emptyList(),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
            return id
        }

        override suspend fun findForUser(sessionId: ChatSessionId, userId: UserId): ChatSession? {
            val session = sessions[sessionId.value] ?: return null
            return session.takeIf { it.userId == userId }
        }

        override suspend fun listRecentTurns(sessionId: ChatSessionId, userId: UserId, limit: Int): List<ChatTurn> {
            val session = findForUser(sessionId, userId) ?: return emptyList()
            return session.turns.takeLast(limit)
        }

        override suspend fun listSessionsForUser(userId: UserId, limit: Int): List<com.bank.banking.domain.model.ChatHistoryEntry> =
            sessions.values
                .filter { it.userId == userId }
                .sortedByDescending { it.updatedAtEpochMs }
                .take(limit)
                .map { session ->
                    val last = session.turns.lastOrNull()
                    com.bank.banking.domain.model.ChatHistoryEntry(
                        sessionId = session.id.value,
                        turnCount = session.turns.size,
                        lastUserMessage = last?.userMessage ?: "",
                        lastIntent = last?.intent,
                        updatedAtEpochMs = session.updatedAtEpochMs,
                    )
                }

        override suspend fun appendTurn(sessionId: ChatSessionId, userId: UserId, turn: ChatTurn) {
            val current = findForUser(sessionId, userId) ?: error("missing session")
            sessions[sessionId.value] = current.copy(
                turns = current.turns + turn,
                updatedAtEpochMs = turn.createdAtEpochMs,
            )
        }

        fun turnCount(sessionId: ChatSessionId): Int =
            sessions[sessionId.value]?.turns?.size ?: 0

        fun lastTurn(sessionId: ChatSessionId): ChatTurn? =
            sessions[sessionId.value]?.turns?.lastOrNull()
    }

    private class TestContext {
        val classifier = RecordingClassifier()
        val chatSessions = InMemoryChatSessions()
        private val sessions = object : SessionRepository {
            override suspend fun create(userId: UserId, token: SessionToken, expiresAt: Instant) = Unit

            override suspend fun findValid(token: SessionToken): SessionRecord? =
                SessionRecord(
                    token = token,
                    userId = UserId("u1"),
                    expiresAt = Instant.fromEpochMilliseconds(Long.MAX_VALUE),
                )

            override suspend fun revoke(token: SessionToken) = Unit
        }

        val useCase = BuildChatUiUseCase(
            sessions = sessions,
            chatSessions = chatSessions,
            routeChatMessage = RouteChatMessageUseCase(classifier),
            builderFactory = BuildChatUiUseCaseSessionTest.chatUiBuilderFactory(sessions),
            clarificationBuilder = ClarificationUiBuilder(),
            supportBuilder = SupportUiBuilder(),
            greetingBuilder = GreetingUiBuilder(
                GetCurrentBalanceUseCase(sessions, chatUiBuilderFactoryAccounts()),
            ),
        )
    }

    private companion object {
        fun chatUiBuilderFactoryAccounts(): AccountRepository =
            object : AccountRepository {
                override suspend fun findPrimaryByUserId(userId: UserId): Account =
                    Account(
                        id = AccountId("acc-1"),
                        userId = userId,
                        balance = Money(25000.0, "PEN"),
                        nickname = "Nomina",
                    )
            }

        fun chatUiBuilderFactory(sessions: SessionRepository): ChatUiBuilderFactory {
        val accounts = chatUiBuilderFactoryAccounts()
        val cardsRepo = object : CreditCardRepository {
            override suspend fun findByIdAndUser(cardId: CreditCardId, userId: UserId): CreditCard? = null

            override suspend fun listWithDebtByUser(userId: UserId): List<CreditCard> =
                listOf(
                    CreditCard(
                        id = CreditCardId("card-1"),
                        userId = userId,
                        alias = "Oro",
                        lastFourDigits = "1234",
                        cardholderName = "MARIA LOPEZ",
                        expiryMonth = 12,
                        expiryYear = 2028,
                        debt = Money(4200.0, "PEN"),
                        creditLine = Money(10_000.0, "PEN"),
                        minimumPaymentDue = Money(420.0, "PEN"),
                        statementBalanceDue = Money(1200.0, "PEN"),
                    ),
                )

            override suspend fun listMovementsByUser(userId: UserId, limit: Int): List<CreditCardMovement> =
                emptyList()

            override suspend fun listMovements(cardId: CreditCardId, userId: UserId, limit: Int): List<CreditCardMovement> =
                emptyList()
        }
        val balanceUseCase = GetCurrentBalanceUseCase(sessions, accounts)
        val payCardsUseCase = ListCreditCardsWithDebtUseCase(sessions, cardsRepo)
        val clarificationBuilder = ClarificationUiBuilder()
        val supportBuilder = SupportUiBuilder()
        val expenses = object : ExpenseRepository {
            override suspend fun summarizeByCategoryForMonth(userId: UserId, yearMonth: String): MonthlyExpenseReport =
                MonthlyExpenseReport(yearMonth, emptyList(), 0, 0.0, "PEN")
        }
        return ChatUiBuilderFactory(
            balance = BalanceUiBuilder(balanceUseCase),
            payCard = PayCreditCardUiBuilder(payCardsUseCase),
            chatHistory = ChatHistoryUiBuilder(ListChatHistoryUseCase(sessions, object : ChatSessionRepository {
                override suspend fun create(userId: UserId) = ChatSessionId("unused")
                override suspend fun findForUser(sessionId: ChatSessionId, userId: UserId) = null
                override suspend fun listRecentTurns(sessionId: ChatSessionId, userId: UserId, limit: Int) = emptyList<ChatTurn>()
                override suspend fun listSessionsForUser(userId: UserId, limit: Int): List<com.bank.banking.domain.model.ChatHistoryEntry> =
                    emptyList()
                override suspend fun appendTurn(sessionId: ChatSessionId, userId: UserId, turn: ChatTurn) = Unit
            })),
            monthlyExpenses = MonthlyExpensesUiBuilder(GetMonthlyExpensesByCategoryUseCase(sessions, expenses)),
            support = supportBuilder,
            clarification = clarificationBuilder,
            notImplementedTransfer = NotImplementedTransferUiBuilder(),
        )
        }
    }
}
