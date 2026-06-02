package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.ListCreditCardsWithDebtUseCase
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.CreditCardRepository
import com.bank.banking.domain.port.SessionRepository
import com.bank.banking.domain.model.sdui.UiComponentType
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PayCreditCardUiBuilderTest {

    @Test
    fun `empty cards returns assistant message`() = runBlocking {
        val token = SessionToken("tok")
        val userId = UserId("u1")

        val sessions = object : SessionRepository {
            override suspend fun create(
                userId: UserId,
                token: SessionToken,
                expiresAt: kotlinx.datetime.Instant,
            ) = Unit

            override suspend fun findValid(token: SessionToken) =
                com.bank.banking.domain.port.SessionRecord(
                    token = token,
                    userId = userId,
                    expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(Long.MAX_VALUE),
                )

            override suspend fun revoke(token: SessionToken) = Unit
        }

        val cardsRepo = object : CreditCardRepository {
            override suspend fun findByIdAndUser(cardId: CreditCardId, userId: UserId): CreditCard? = null
            override suspend fun listWithDebtByUser(userId: UserId): List<CreditCard> = emptyList()
            override suspend fun listMovementsByUser(userId: UserId, limit: Int): List<com.bank.banking.domain.model.CreditCardMovement> = emptyList()
            override suspend fun listMovements(cardId: CreditCardId, userId: UserId, limit: Int): List<com.bank.banking.domain.model.CreditCardMovement> = emptyList()
        }

        val useCase = ListCreditCardsWithDebtUseCase(sessions, cardsRepo)
        val builder = PayCreditCardUiBuilder(useCase)

        val ctx = ChatUiBuildContext(
            token = token,
            userMessage = "pagar tc",
            classification = com.bank.banking.domain.model.IntentClassification(
                intent = com.bank.banking.domain.model.IntentLabel.PAY_CREDIT_CARD,
                confidence = 0.9,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "test",
                source = "test",
            ),
        )

        val tree = builder.build(ctx)
        assertEquals(UiComponentType.COLUMN, tree.type)

        val assistant = tree.children.single { it.type == UiComponentType.ASSISTANT_TEXT }
        assertNotNull(assistant.props["text"])
        assertEquals("No tienes tarjetas con deuda pendiente.", assistant.props["text"])
    }

    @Test
    fun `non-empty cards returns pay panel nodes with card props`() = runBlocking {
        val token = SessionToken("tok")
        val userId = UserId("u1")
        val cardId = CreditCardId("card-1")

        val sessions = object : SessionRepository {
            override suspend fun create(
                userId: UserId,
                token: SessionToken,
                expiresAt: kotlinx.datetime.Instant,
            ) = Unit

            override suspend fun findValid(token: SessionToken) =
                com.bank.banking.domain.port.SessionRecord(
                    token = token,
                    userId = userId,
                    expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(Long.MAX_VALUE),
                )

            override suspend fun revoke(token: SessionToken) = Unit
        }

        val cardsRepo = object : CreditCardRepository {
            override suspend fun findByIdAndUser(cardId: CreditCardId, userId: UserId): CreditCard? = null
            override suspend fun listWithDebtByUser(userId: UserId): List<CreditCard> =
                listOf(
                    CreditCard(
                        id = cardId,
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

            override suspend fun listMovementsByUser(userId: UserId, limit: Int): List<com.bank.banking.domain.model.CreditCardMovement> = emptyList()
            override suspend fun listMovements(cardId: CreditCardId, userId: UserId, limit: Int): List<com.bank.banking.domain.model.CreditCardMovement> = emptyList()
        }

        val useCase = ListCreditCardsWithDebtUseCase(sessions, cardsRepo)
        val builder = PayCreditCardUiBuilder(useCase)

        val ctx = ChatUiBuildContext(
            token = token,
            userMessage = "pagar tc",
            classification = com.bank.banking.domain.model.IntentClassification(
                intent = com.bank.banking.domain.model.IntentLabel.PAY_CREDIT_CARD,
                confidence = 0.9,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "test",
                source = "test",
            ),
        )

        val tree = builder.build(ctx)
        assertEquals(UiComponentType.COLUMN, tree.type)

        val payPanels = tree.children.filter { it.type == UiComponentType.PAY_CARD_PANEL }
        assertEquals(1, payPanels.size)

        val panel = payPanels.single()
        assertEquals("card-1", panel.props["cardId"])
        assertEquals("Oro", panel.props["alias"])
        assertEquals("1234", panel.props["lastFourDigits"])
        assertTrue(panel.props.containsKey("debt"))
    }
}

