package com.bank.banking.domain.port

import com.bank.banking.domain.model.MonthlyExpenseReport
import com.bank.banking.domain.model.UserId

interface ExpenseRepository {
    suspend fun summarizeByCategoryForMonth(userId: UserId, yearMonth: String): MonthlyExpenseReport
}
