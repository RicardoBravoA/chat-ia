package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.GetCurrentBalanceUseCase
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.sdui.UiNode

data class GreetingQuickReply(
    val label: String,
    val intent: IntentLabel,
)

class GreetingUiBuilder(
    private val balanceUseCase: GetCurrentBalanceUseCase,
) : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode {
        val nickname = balanceUseCase.execute(ctx.token).nickname
        return SduiNodeFactory.column(
            "greeting-root",
            SduiNodeFactory.greetingCard(
                id = "greeting-card",
                message = GroundedChatCopy.greetingIntro(nickname),
                quickReplies = QUICK_REPLIES,
            ),
        )
    }

    companion object {
        val QUICK_REPLIES: List<GreetingQuickReply> = listOf(
            GreetingQuickReply("Ver saldo", IntentLabel.CHECK_BALANCE),
            GreetingQuickReply("Pagar tarjeta", IntentLabel.PAY_CREDIT_CARD),
            GreetingQuickReply("Gastos del mes", IntentLabel.MONTHLY_EXPENSES),
            GreetingQuickReply("Historial chat", IntentLabel.VIEW_CHAT_HISTORY),
            GreetingQuickReply("Transferir", IntentLabel.TRANSFER_OWN_ACCOUNTS),
        )
    }
}
