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

    fun greetingCard(
        id: String,
        message: String,
        quickReplies: List<GreetingQuickReply>,
    ): UiNode =
        UiNode(
            id = id,
            type = UiComponentType.GREETING_CARD,
            props = mapOf("message" to message),
            actions = quickReplies.mapIndexed { index, reply ->
                UiAction(
                    id = "quick-reply-$index",
                    label = reply.label,
                    actionType = "QUICK_REPLY",
                    payload = mapOf("intent" to reply.intent.name),
                    requiresConfirmation = false,
                )
            },
        )

    fun chatHistoryRow(
        id: String,
        sessionLabel: String,
        lastMessage: String,
        turnCount: Int,
        timeLabel: String,
        lastIntent: String,
    ): UiNode =
        UiNode(
            id = id,
            type = UiComponentType.CHAT_HISTORY_ROW,
            props = mapOf(
                "sessionLabel" to sessionLabel,
                "lastMessage" to lastMessage,
                "turnCount" to turnCount.toString(),
                "timeLabel" to timeLabel,
                "lastIntent" to lastIntent,
            ),
        )

    fun spendingCategoryRow(
        id: String,
        category: String,
        transactionCount: Int,
        totalAmountFormatted: String,
    ): UiNode =
        UiNode(
            id = id,
            type = UiComponentType.SPENDING_CATEGORY_ROW,
            props = mapOf(
                "category" to category,
                "transactionCount" to transactionCount.toString(),
                "totalAmountFormatted" to totalAmountFormatted,
            ),
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
