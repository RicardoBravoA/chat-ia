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
            SduiNodeFactory.assistantText("balance-intro", "Tu saldo disponible es:"),
            SduiNodeFactory.balanceCard("balance-card", formatted, result.balance.currency),
            SduiNodeFactory.assistantText(
                "balance-follow-up",
                "¿Te gustaría realizar alguna transferencia o ver tus movimientos recientes?",
            ),
        )
    }
}
