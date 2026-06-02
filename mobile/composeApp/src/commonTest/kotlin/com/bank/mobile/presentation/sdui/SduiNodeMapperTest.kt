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
