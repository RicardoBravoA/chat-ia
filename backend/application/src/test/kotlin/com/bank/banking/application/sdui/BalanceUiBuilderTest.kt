package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.BalanceQueryResult
import com.bank.banking.application.usecase.GetCurrentBalanceUseCase
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.sdui.UiComponentType
import com.bank.banking.domain.port.AccountRepository
import com.bank.banking.domain.port.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BalanceUiBuilderTest {
    @Test
    fun `builds column with balance card`() = runBlocking {
        val token = SessionToken("tok")
        val sessions = object : SessionRepository {
            override suspend fun create(
                userId: com.bank.banking.domain.model.UserId,
                token: SessionToken,
                expiresAt: kotlinx.datetime.Instant,
            ) {}

            override suspend fun findValid(token: SessionToken) =
                com.bank.banking.domain.port.SessionRecord(
                    token = token,
                    userId = com.bank.banking.domain.model.UserId("u1"),
                    expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(Long.MAX_VALUE),
                )

            override suspend fun revoke(token: SessionToken) {}
        }
        val accounts = object : AccountRepository {
            override suspend fun findPrimaryByUserId(userId: com.bank.banking.domain.model.UserId) =
                com.bank.banking.domain.model.Account(
                    id = com.bank.banking.domain.model.AccountId("ACC-001"),
                    userId = userId,
                    balance = Money(25000.0, "PEN"),
                    nickname = "Nómina",
                )
        }
        val balanceUseCase = GetCurrentBalanceUseCase(sessions, accounts)
        val tree = BalanceUiBuilder(balanceUseCase).build(
            ChatUiBuildContext(
                token = token,
                userMessage = "saldo",
                classification = com.bank.banking.domain.model.IntentClassification(
                    intent = com.bank.banking.domain.model.IntentLabel.CHECK_BALANCE,
                    confidence = 0.95,
                    entities = emptyMap(),
                    clarificationNeeded = false,
                    reason = "test",
                    source = "test",
                ),
            ),
        )
        assertEquals(UiComponentType.COLUMN, tree.type)
        assertTrue(tree.children.any { it.type == UiComponentType.BALANCE_CARD })
        assertTrue(tree.children.any { it.type == UiComponentType.ASSISTANT_TEXT })
    }
}
