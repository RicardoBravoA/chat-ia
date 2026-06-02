package com.bank.banking.application.sdui

import com.bank.banking.domain.model.sdui.UiAction
import com.bank.banking.domain.model.sdui.UiComponentType
import com.bank.banking.domain.model.sdui.UiNode

internal object SduiNodeFactory {
    fun column(id: String, vararg children: UiNode): UiNode =
        UiNode(id = id, type = UiComponentType.COLUMN, children = children.toList())

    fun assistantText(id: String, text: String): UiNode =
        UiNode(
            id = id,
            type = UiComponentType.ASSISTANT_TEXT,
            props = mapOf("text" to text),
        )

    fun balanceCard(id: String, amountFormatted: String, currency: String): UiNode =
        UiNode(
            id = id,
            type = UiComponentType.BALANCE_CARD,
            props = mapOf(
                "amountFormatted" to amountFormatted,
                "currency" to currency,
            ),
        )

    fun payCardPanel(
        id: String,
        props: Map<String, String>,
    ): UiNode = UiNode(id = id, type = UiComponentType.PAY_CARD_PANEL, props = props)

    fun supportChannelsCard(id: String): UiNode =
        UiNode(id = id, type = UiComponentType.SUPPORT_CHANNELS_CARD)

    fun infoBanner(id: String, text: String): UiNode =
        UiNode(
            id = id,
            type = UiComponentType.INFO_BANNER,
            props = mapOf("text" to text),
        )

    fun payCardAction(cardId: String, paymentMode: String): UiAction =
        UiAction(
            id = "pay-$cardId-$paymentMode",
            label = "Pagar",
            actionType = "PAY_CARD",
            payload = mapOf(
                "cardId" to cardId,
                "paymentMode" to paymentMode,
            ),
            requiresConfirmation = true,
        )
}
