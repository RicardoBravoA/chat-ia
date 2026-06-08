package com.bank.mobile.presentation.sdui

import com.bank.mobile.domain.model.sdui.UiComponentType
import com.bank.mobile.domain.model.sdui.UiNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SduiNodeMapperTest {
    @Test
    fun mapsGreetingCardQuickReplies() {
        val node = UiNode(
            id = "g1",
            type = UiComponentType.GREETING_CARD,
            props = mapOf("message" to "Hola"),
            actions = listOf(
                com.bank.mobile.domain.model.sdui.UiAction(
                    id = "q1",
                    label = "Ver saldo",
                    actionType = "QUICK_REPLY",
                    payload = mapOf("intent" to "CHECK_BALANCE"),
                ),
            ),
        )

        assertEquals("Hola", node.propText("message"))
        assertEquals(1, node.actions.size)
        assertEquals("CHECK_BALANCE", node.actions.first().payload["intent"])
    }

    @Test
    fun mapsPayCardPanelPropsToAction() {
        val node = UiNode(
            id = "p1",
            type = UiComponentType.PAY_CARD_PANEL,
            props = mapOf(
                "cardId" to "CARD-001",
                "alias" to "Oro",
                "lastFourDigits" to "1234",
                "cardholderName" to "MARIA LOPEZ",
                "expiryMonth" to "12",
                "expiryYear" to "2028",
                "debt" to "4200.0",
                "creditLine" to "10000.0",
                "minimumPaymentDue" to "420.0",
                "statementBalanceDue" to "1200.0",
                "currency" to "PEN",
            ),
        )
        val action = node.toPayCardChatAction()
        assertNotNull(action)
        assertEquals("CARD-001", action.cardId)
        assertEquals(4200.0, action.debt)
    }

    @Test
    fun flattensColumnChildren() {
        val tree = UiNode(
            id = "root",
            type = UiComponentType.COLUMN,
            children = listOf(
                UiNode("t1", UiComponentType.ASSISTANT_TEXT, props = mapOf("text" to "hola")),
                UiNode("b1", UiComponentType.BALANCE_CARD, props = mapOf("amountFormatted" to "100 PEN")),
            ),
        )
        val flat = tree.flattenForChat()
        assertEquals(2, flat.size)
    }

    @Test
    fun mapsChatHistoryRowProps() {
        val node = UiNode(
            id = "h1",
            type = UiComponentType.CHAT_HISTORY_ROW,
            props = mapOf(
                "sessionLabel" to "Conversación 1",
                "lastMessage" to "ver saldo",
                "turnCount" to "2",
                "timeLabel" to "01/06/2024 10:00",
                "lastIntent" to "CHECK_BALANCE",
            ),
        )
        assertEquals("Conversación 1", node.propText("sessionLabel"))
        assertEquals(2, node.propInt("turnCount"))
    }

    @Test
    fun mapsSpendingCategoryRowProps() {
        val node = UiNode(
            id = "s1",
            type = UiComponentType.SPENDING_CATEGORY_ROW,
            props = mapOf(
                "category" to "Alimentación",
                "transactionCount" to "3",
                "totalAmountFormatted" to "96.25 PEN",
            ),
        )
        assertEquals("Alimentación", node.propText("category"))
        assertEquals(3, node.propInt("transactionCount"))
    }

    @Test
    fun returnsNullWhenNodeIsNotPayCardPanel() {
        val node = UiNode(
            id = "x",
            type = UiComponentType.ASSISTANT_TEXT,
            props = mapOf("cardId" to "CARD-001"),
        )

        assertNull(node.toPayCardChatAction())
    }

    @Test
    fun returnsNullWhenExpiryMonthIsInvalid() {
        val node = UiNode(
            id = "p1",
            type = UiComponentType.PAY_CARD_PANEL,
            props = mapOf(
                "cardId" to "CARD-001",
                "alias" to "Oro",
                "lastFourDigits" to "1234",
                "cardholderName" to "MARIA LOPEZ",
                "expiryMonth" to "xx",
                "expiryYear" to "2028",
                "debt" to "4200.0",
                "creditLine" to "10000.0",
                "minimumPaymentDue" to "420.0",
                "statementBalanceDue" to "1200.0",
                "currency" to "PEN",
            ),
        )

        assertNull(node.toPayCardChatAction())
    }

    @Test
    fun propTextMissingReturnsEmptyString() {
        val node = UiNode(
            id = "x",
            type = UiComponentType.ASSISTANT_TEXT,
            props = emptyMap(),
        )

        assertEquals("", node.propText("missing"))
    }

    @Test
    fun flattensNonColumnNodesAsSingleNode() {
        val node = UiNode(
            id = "x",
            type = UiComponentType.ASSISTANT_TEXT,
            props = mapOf("text" to "hola"),
        )

        val flat = node.flattenForChat()
        assertEquals(1, flat.size)
        assertTrue(flat.first().id == "x")
    }
}
