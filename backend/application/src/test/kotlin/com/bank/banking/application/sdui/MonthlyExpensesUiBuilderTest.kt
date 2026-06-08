package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.GetMonthlyExpensesByCategoryUseCase
import com.bank.banking.domain.model.ExpenseCategorySummary
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.MonthlyExpenseReport
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.model.sdui.UiComponentType
import com.bank.banking.domain.port.ExpenseRepository
import com.bank.banking.domain.port.SessionRecord
import com.bank.banking.domain.port.SessionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonthlyExpensesUiBuilderTest {
    @Test
    fun `builds spending rows grouped by category`() = runBlocking {
        val token = SessionToken("tok")
        val userId = UserId("u1")
        val sessions = object : SessionRepository {
            override suspend fun create(userId: UserId, token: SessionToken, expiresAt: Instant) = Unit
            override suspend fun findValid(token: SessionToken) =
                SessionRecord(token, userId, Instant.fromEpochMilliseconds(Long.MAX_VALUE))
            override suspend fun revoke(token: SessionToken) = Unit
        }
        val expenses = object : ExpenseRepository {
            override suspend fun summarizeByCategoryForMonth(userId: UserId, yearMonth: String): MonthlyExpenseReport =
                MonthlyExpenseReport(
                    yearMonth = yearMonth,
                    categories = listOf(
                        ExpenseCategorySummary("cat_food", "Alimentación", 3, 96.25, "PEN"),
                        ExpenseCategorySummary("cat_transport", "Transporte", 2, 20.5, "PEN"),
                    ),
                    totalTransactionCount = 5,
                    grandTotal = 116.75,
                    currency = "PEN",
                )
        }
        val tree = MonthlyExpensesUiBuilder(GetMonthlyExpensesByCategoryUseCase(sessions, expenses)).build(
            ChatUiBuildContext(
                token = token,
                userMessage = "gastos del mes",
                classification = IntentClassification(
                    intent = IntentLabel.MONTHLY_EXPENSES,
                    confidence = 0.95,
                    entities = emptyMap(),
                    clarificationNeeded = false,
                    reason = "test",
                    source = "test",
                ),
            ),
        )
        assertEquals(UiComponentType.COLUMN, tree.type)
        assertEquals(2, tree.children.count { it.type == UiComponentType.SPENDING_CATEGORY_ROW })
        assertTrue(tree.children.any { it.props["category"] == "Alimentación" })
    }
}
