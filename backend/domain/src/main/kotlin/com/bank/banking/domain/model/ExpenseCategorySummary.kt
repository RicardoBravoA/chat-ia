package com.bank.banking.domain.model

data class ExpenseCategorySummary(
    val categoryId: String,
    val categoryLabel: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val currency: String,
)

data class MonthlyExpenseReport(
    val yearMonth: String,
    val categories: List<ExpenseCategorySummary>,
    val totalTransactionCount: Int,
    val grandTotal: Double,
    val currency: String,
)
