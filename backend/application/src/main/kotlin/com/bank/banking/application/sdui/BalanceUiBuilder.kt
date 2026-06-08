package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.GetCurrentBalanceUseCase
import com.bank.banking.domain.model.sdui.UiNode

class BalanceUiBuilder(
    private val balanceUseCase: GetCurrentBalanceUseCase,
) : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode {
        val result = balanceUseCase.execute(ctx.token)
        val formatted = result.balance.formatForDisplay()
        return SduiNodeFactory.column(
            "balance-root",
            SduiNodeFactory.assistantText(
                "balance-intro",
                GroundedChatCopy.balanceIntro(result.nickname, formatted),
            ),
            SduiNodeFactory.balanceCard("balance-card", formatted, result.balance.currency),
            SduiNodeFactory.assistantText(
                "balance-follow-up",
                GroundedChatCopy.balanceFollowUp(),
            ),
        )
    }
}
