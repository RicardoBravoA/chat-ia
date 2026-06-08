package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.GetMonthlyExpensesByCategoryUseCase
import com.bank.banking.domain.model.sdui.UiNode

class MonthlyExpensesUiBuilder(
    private val monthlyExpenses: GetMonthlyExpensesByCategoryUseCase,
) : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode {
        val yearMonth = YearMonthResolver.resolve(
            entities = ctx.classification.entities,
            userMessage = ctx.userMessage,
        )
        val report = monthlyExpenses.execute(ctx.token, yearMonth)
        val monthLabel = YearMonthResolver.formatForDisplay(report.yearMonth)

        if (report.categories.isEmpty()) {
            return SduiNodeFactory.column(
                "monthly-expenses-empty",
                SduiNodeFactory.assistantText(
                    "monthly-expenses-empty-text",
                    GroundedChatCopy.monthlyExpensesEmpty(monthLabel),
                ),
            )
        }

        val children = mutableListOf<UiNode>(
            SduiNodeFactory.assistantText(
                "monthly-expenses-intro",
                GroundedChatCopy.monthlyExpensesIntro(
                    monthLabel = monthLabel,
                    totalTransactions = report.totalTransactionCount,
                    grandTotal = report.grandTotal,
                    currency = report.currency,
                ),
            ),
        )
        report.categories.forEachIndexed { index, category ->
            children += SduiNodeFactory.spendingCategoryRow(
                id = "spending-row-$index",
                category = category.categoryLabel,
                transactionCount = category.transactionCount,
                totalAmountFormatted = formatAmount(category.totalAmount, category.currency),
            )
        }
        children += SduiNodeFactory.assistantText(
            "monthly-expenses-follow-up",
            GroundedChatCopy.monthlyExpensesFollowUp(),
        )
        return SduiNodeFactory.column("monthly-expenses-root", *children.toTypedArray())
    }

    private fun formatAmount(amount: Double, currency: String): String {
        val rounded = kotlin.math.round(amount * 100.0) / 100.0
        return "$rounded $currency"
    }
}
