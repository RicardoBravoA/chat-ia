package com.bank.banking.application.sdui

import com.bank.banking.application.usecase.ListCreditCardsWithDebtUseCase
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.sdui.UiNode

class PayCreditCardUiBuilder(
    private val cardsUseCase: ListCreditCardsWithDebtUseCase,
) : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode {
        val cards = cardsUseCase.execute(ctx.token)
        if (cards.isEmpty()) {
            return SduiNodeFactory.column(
                "pay-empty",
                SduiNodeFactory.assistantText(
                    "pay-empty-text",
                    GroundedChatCopy.payCardsEmpty(),
                ),
            )
        }
        return SduiNodeFactory.column(
            "pay-root",
            SduiNodeFactory.assistantText(
                "pay-intro",
                GroundedChatCopy.payCardsIntro(cards, ctx.classification.entities),
            ),
            *cards.mapIndexed { index, card -> card.toPayCardPanelNode("pay-card-$index") }.toTypedArray(),
        )
    }

    private fun CreditCard.toPayCardPanelNode(nodeId: String): UiNode {
        val props = mapOf(
            "cardId" to id.value,
            "alias" to alias,
            "lastFourDigits" to lastFourDigits,
            "cardholderName" to cardholderName,
            "expiryMonth" to expiryMonth.toString(),
            "expiryYear" to expiryYear.toString(),
            "debt" to debt.amount.toString(),
            "creditLine" to creditLine.amount.toString(),
            "minimumPaymentDue" to minimumPaymentDue.amount.toString(),
            "statementBalanceDue" to statementBalanceDue.amount.toString(),
            "currency" to debt.currency,
        )
        return SduiNodeFactory.payCardPanel(nodeId, props)
    }
}
