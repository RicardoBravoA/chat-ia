package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.GetCurrentBalanceUseCase
import com.bank.banking.domain.model.Account
import com.bank.banking.domain.model.AccountId
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.model.sdui.UiComponentType
import com.bank.banking.domain.port.AccountRepository
import com.bank.banking.domain.port.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GreetingUiBuilderTest {
    @Test
    fun `builds greeting card with quick reply actions`() = runBlocking {
        val token = SessionToken("tok")
        val balanceUseCase = GetCurrentBalanceUseCase(
            sessions = fakeSessions(token),
            accounts = fakeAccounts(nickname = "María"),
        )
        val tree = GreetingUiBuilder(balanceUseCase).build(
            ChatUiBuildContext(
                token = token,
                userMessage = "hola",
                classification = com.bank.banking.domain.model.IntentClassification(
                    intent = com.bank.banking.domain.model.IntentLabel.AMBIGUOUS,
                    confidence = 0.4,
                    entities = emptyMap(),
                    clarificationNeeded = true,
                    reason = "test",
                    source = "test",
                ),
            ),
        )

        assertEquals(UiComponentType.COLUMN, tree.type)
        val greeting = tree.children.single()
        assertEquals(UiComponentType.GREETING_CARD, greeting.type)
        assertEquals(GroundedChatCopy.greetingIntro("María"), greeting.props["message"])
        assertEquals(GreetingUiBuilder.QUICK_REPLIES.size, greeting.actions.size)
        assertTrue(greeting.actions.all { it.actionType == "QUICK_REPLY" })
        assertEquals("Ver saldo", greeting.actions[0].label)
        assertEquals("CHECK_BALANCE", greeting.actions[0].payload["intent"])
    }

    private fun fakeSessions(token: SessionToken): SessionRepository =
        object : SessionRepository {
            override suspend fun create(
                userId: UserId,
                token: SessionToken,
                expiresAt: kotlinx.datetime.Instant,
            ) = Unit

            override suspend fun findValid(token: SessionToken) =
                com.bank.banking.domain.port.SessionRecord(
                    token = token,
                    userId = UserId("u1"),
                    expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(Long.MAX_VALUE),
                )

            override suspend fun revoke(token: SessionToken) = Unit
        }

    private fun fakeAccounts(nickname: String): AccountRepository =
        object : AccountRepository {
            override suspend fun findPrimaryByUserId(userId: UserId): Account =
                Account(
                    id = AccountId("acc-1"),
                    userId = userId,
                    balance = Money(25000.0, "PEN"),
                    nickname = nickname,
                )
        }
}
