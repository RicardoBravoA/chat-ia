package com.bank.banking.application.usecase

import com.bank.banking.application.sdui.BalanceUiBuilder
import com.bank.banking.application.sdui.ChatHistoryUiBuilder
import com.bank.banking.application.sdui.ChatUiBuilderFactory
import com.bank.banking.application.sdui.ClarificationUiBuilder
import com.bank.banking.application.sdui.GreetingUiBuilder
import com.bank.banking.application.sdui.MonthlyExpensesUiBuilder
import com.bank.banking.application.sdui.PayCreditCardUiBuilder
import com.bank.banking.application.sdui.SupportUiBuilder
import com.bank.banking.application.sdui.NotImplementedTransferUiBuilder
import com.bank.banking.domain.model.MonthlyExpenseReport
import com.bank.banking.domain.port.ExpenseRepository
import com.bank.banking.application.usecase.ListCreditCardsWithDebtUseCase
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.model.Account
import com.bank.banking.domain.model.AccountId
import com.bank.banking.domain.model.ChatSession
import com.bank.banking.domain.model.ChatSessionId
import com.bank.banking.domain.model.ChatTurn
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.sdui.UiComponentType
import com.bank.banking.domain.port.AccountRepository
import com.bank.banking.domain.port.ChatSessionRepository
import com.bank.banking.domain.port.CreditCardRepository
import com.bank.banking.domain.port.SessionRepository
import com.bank.banking.domain.port.SessionRecord
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

class BuildChatUiUseCaseTest {

    @Test
    fun `quick reply selectedIntent skips classifier and builds target ui`() = runBlocking {
        val token = SessionToken("tok")
        val ctx = TestChatContext(
            classification = classification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.1,
                clarificationNeeded = true,
            ),
        )

        val response = ctx.useCase.execute(
            token = token,
            message = "Ver saldo",
            sessionId = null,
            selectedIntent = "CHECK_BALANCE",
        )

        assertTrue(response.uiTree.children.any { it.type == UiComponentType.BALANCE_CARD })
        assertEquals("quick_reply", response.metadata.routerSource)
        assertEquals("CHECK_BALANCE", response.metadata.intent)
    }

    @Test
    fun `greeting message shows greeting card instead of clarification`() = runBlocking {
        val token = SessionToken("tok")
        val ctx = TestChatContext(
            classification = classification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.3,
                clarificationNeeded = true,
            ),
        )

        val tree = ctx.useCase.execute(token, "hola").uiTree

        assertEquals(UiComponentType.COLUMN, tree.type)
        assertEquals(UiComponentType.GREETING_CARD, tree.children.single().type)
    }

    @Test
    fun `AMBIGUOUS asks clarification`() = runBlocking {
        val token = SessionToken("tok")
        val ctx = TestChatContext(
            classification = classification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.99,
                clarificationNeeded = false,
            ),
        )

        val tree = ctx.useCase.execute(token, "no se que necesito exactamente").uiTree

        assertEquals(UiComponentType.COLUMN, tree.type)
        assertTrue(tree.children.any { it.type == UiComponentType.ASSISTANT_TEXT })
        val assistant = tree.children.single { it.type == UiComponentType.ASSISTANT_TEXT }
        assertEquals(ClarificationUiBuilder.CLARIFICATION_MESSAGE, assistant.props["text"])
    }

    @Test
    fun `OUT_OF_SCOPE low confidence rechecks out of scope by showing clarification`() = runBlocking {
        val token = SessionToken("tok")
        val ctx = TestChatContext(
            classification = classification(
                intent = IntentLabel.OUT_OF_SCOPE,
                confidence = 0.4,
                clarificationNeeded = false,
            ),
        )

        val tree = ctx.useCase.execute(token, "Quiero ver saldo").uiTree

        assertEquals(UiComponentType.COLUMN, tree.type)
        val assistant = tree.children.single { it.type == UiComponentType.ASSISTANT_TEXT }
        assertEquals(ClarificationUiBuilder.CLARIFICATION_MESSAGE, assistant.props["text"])
    }

    @Test
    fun `OUT_OF_SCOPE high confidence shows support`() = runBlocking {
        val token = SessionToken("tok")
        val ctx = TestChatContext(
            classification = classification(
                intent = IntentLabel.OUT_OF_SCOPE,
                confidence = 0.9,
                clarificationNeeded = false,
            ),
        )

        val tree = ctx.useCase.execute(token, "clima hoy").uiTree

        assertEquals(UiComponentType.COLUMN, tree.type)
        assertTrue(tree.children.any { it.type == UiComponentType.SUPPORT_CHANNELS_CARD })
    }

    @Test
    fun `CHECK_BALANCE builds balance card`() = runBlocking {
        val token = SessionToken("tok")
        val ctx = TestChatContext(
            classification = classification(
                intent = IntentLabel.CHECK_BALANCE,
                confidence = 0.9,
                clarificationNeeded = false,
            ),
        )

        val tree = ctx.useCase.execute(token, "quiero ver saldos").uiTree

        assertEquals(UiComponentType.COLUMN, tree.type)
        assertTrue(tree.children.any { it.type == UiComponentType.BALANCE_CARD })
        val balanceCard = tree.children.first { it.type == UiComponentType.BALANCE_CARD }
        assertEquals("PEN", balanceCard.props["currency"])
        assertTrue(balanceCard.props["amountFormatted"].orEmpty().isNotBlank())
    }

    @Test
    fun `PAY_CREDIT_CARD builds pay card panels`() = runBlocking {
        val token = SessionToken("tok")
        val ctx = TestChatContext(
            classification = classification(
                intent = IntentLabel.PAY_CREDIT_CARD,
                confidence = 0.9,
                clarificationNeeded = false,
            ),
        )

        val resp = ctx.useCase.execute(token, "pagar tc")
        val tree = resp.uiTree

        assertEquals(1, resp.schemaVersion)
        assertTrue(resp.correlationId.isNotBlank())

        assertEquals(UiComponentType.COLUMN, tree.type)
        val payPanels = tree.children.filter { it.type == UiComponentType.PAY_CARD_PANEL }
        assertEquals(1, payPanels.size)
        assertEquals("card-1", payPanels.single().props["cardId"])
    }

    private fun classification(
        intent: IntentLabel,
        confidence: Double,
        clarificationNeeded: Boolean,
    ): IntentClassification =
        IntentClassification(
            intent = intent,
            confidence = confidence,
            entities = emptyMap(),
            clarificationNeeded = clarificationNeeded,
            reason = "test",
            source = "test",
        )

    private class TestChatContext(
        classification: IntentClassification,
    ) {
        // RouteChatMessageUseCase depends on an IntentClassifierPort; we stub classify() directly.
        private val intentClassifier = object : com.bank.banking.domain.port.IntentClassifierPort {
            override suspend fun classify(
                message: String,
                history: List<com.bank.banking.domain.model.ChatHistoryMessage>,
            ) = classification
        }

        private val routeChatMessage = RouteChatMessageUseCase(intentClassifier)

        private val sessions = object : SessionRepository {
            override suspend fun create(
                userId: UserId,
                token: SessionToken,
                expiresAt: kotlinx.datetime.Instant,
            ) = Unit

            override suspend fun findValid(token: SessionToken): SessionRecord? =
                SessionRecord(
                    token = token,
                    userId = UserId("u1"),
                    expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(Long.MAX_VALUE),
                )

            override suspend fun revoke(token: SessionToken) = Unit
        }

        private val accounts = object : AccountRepository {
            override suspend fun findPrimaryByUserId(userId: UserId): Account =
                Account(
                    id = AccountId("acc-1"),
                    userId = userId,
                    balance = Money(25000.0, "PEN"),
                    nickname = "Nómina",
                )
        }

        private val cardsRepo = object : CreditCardRepository {
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

            override suspend fun listMovementsByUser(userId: UserId, limit: Int) =
                emptyList<com.bank.banking.domain.model.CreditCardMovement>()

            override suspend fun listMovements(cardId: CreditCardId, userId: UserId, limit: Int) =
                emptyList<com.bank.banking.domain.model.CreditCardMovement>()
        }

        private val balanceUseCase = GetCurrentBalanceUseCase(sessions, accounts)
        private val payCardsUseCase = ListCreditCardsWithDebtUseCase(sessions, cardsRepo)

        private val balanceBuilder = BalanceUiBuilder(balanceUseCase)
        private val payBuilder = PayCreditCardUiBuilder(payCardsUseCase)

        val clarificationBuilder = ClarificationUiBuilder()
        private val greetingBuilder = GreetingUiBuilder(balanceUseCase)
        private val supportBuilder = SupportUiBuilder()
        private val notImplementedTransferBuilder = NotImplementedTransferUiBuilder()

        private val chatSessions = object : ChatSessionRepository {
            private val sessions = ConcurrentHashMap<String, ChatSession>()

            override suspend fun create(userId: UserId): ChatSessionId {
                val id = ChatSessionId("test-session")
                sessions[id.value] = ChatSession(id, userId, emptyList(), System.currentTimeMillis())
                return id
            }

            override suspend fun findForUser(sessionId: ChatSessionId, userId: UserId): ChatSession? =
                sessions[sessionId.value]?.takeIf { it.userId == userId }

            override suspend fun listRecentTurns(sessionId: ChatSessionId, userId: UserId, limit: Int): List<ChatTurn> =
                findForUser(sessionId, userId)?.turns?.takeLast(limit) ?: emptyList()

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
                val current = findForUser(sessionId, userId) ?: return
                sessions[sessionId.value] = current.copy(turns = current.turns + turn)
            }
        }

        val useCase = BuildChatUiUseCase(
            sessions = sessions,
            chatSessions = chatSessions,
            routeChatMessage = routeChatMessage,
            builderFactory = chatUiBuilderFactory(
                sessions = sessions,
                chatSessions = chatSessions,
                balanceBuilder = balanceBuilder,
                payBuilder = payBuilder,
                supportBuilder = supportBuilder,
                clarificationBuilder = clarificationBuilder,
                notImplementedTransferBuilder = notImplementedTransferBuilder,
            ),
            clarificationBuilder = clarificationBuilder,
            supportBuilder = supportBuilder,
            greetingBuilder = greetingBuilder,
        )

        fun chatUiBuilderFactory(
            sessions: SessionRepository,
            chatSessions: ChatSessionRepository,
            balanceBuilder: BalanceUiBuilder,
            payBuilder: PayCreditCardUiBuilder,
            supportBuilder: SupportUiBuilder,
            clarificationBuilder: ClarificationUiBuilder,
            notImplementedTransferBuilder: NotImplementedTransferUiBuilder,
        ): ChatUiBuilderFactory {
            val expenses = object : ExpenseRepository {
                override suspend fun summarizeByCategoryForMonth(
                    userId: UserId,
                    yearMonth: String,
                ): MonthlyExpenseReport =
                    MonthlyExpenseReport(
                        yearMonth = yearMonth,
                        categories = emptyList(),
                        totalTransactionCount = 0,
                        grandTotal = 0.0,
                        currency = "PEN",
                    )
            }
            return ChatUiBuilderFactory(
                balance = balanceBuilder,
                payCard = payBuilder,
                chatHistory = ChatHistoryUiBuilder(ListChatHistoryUseCase(sessions, chatSessions)),
                monthlyExpenses = MonthlyExpensesUiBuilder(GetMonthlyExpensesByCategoryUseCase(sessions, expenses)),
                support = supportBuilder,
                clarification = clarificationBuilder,
                notImplementedTransfer = notImplementedTransferBuilder,
            )
        }
    }
}

