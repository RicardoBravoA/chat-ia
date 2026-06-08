package com.bank.banking.application.usecase

import com.bank.banking.application.sdui.YearMonthResolver
import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.MonthlyExpenseReport
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.ExpenseRepository
import com.bank.banking.domain.port.SessionRepository

class GetMonthlyExpensesByCategoryUseCase(
    private val sessions: SessionRepository,
    private val expenses: ExpenseRepository,
) {
    suspend fun execute(token: SessionToken, yearMonth: String? = null): MonthlyExpenseReport {
        val userId = sessions.findValid(token)?.userId
            ?: throw UnauthorizedException("Invalid or expired session")
        val resolvedMonth = yearMonth?.trim()?.takeIf { it.isNotEmpty() }
            ?: YearMonthResolver.currentYearMonth()
        return expenses.summarizeByCategoryForMonth(userId, resolvedMonth)
    }
}
