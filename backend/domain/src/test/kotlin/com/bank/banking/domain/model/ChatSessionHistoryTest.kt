package com.bank.banking.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatSessionHistoryTest {
    @Test
    fun `turns map to alternating user and assistant history with classification json`() {
        val turns = listOf(
            ChatTurn(
                userMessage = "cuanto debo en la oro",
                intent = "CHECK_BALANCE",
                correlationId = "c1",
                createdAtEpochMs = 1L,
                confidence = 0.91,
                reason = "consulta deuda tarjeta oro",
                routerSource = "woz",
                entities = mapOf("cardAlias" to "oro"),
            ),
            ChatTurn(
                userMessage = "paga 50",
                intent = "PAY_CREDIT_CARD",
                correlationId = "c2",
                createdAtEpochMs = 2L,
                confidence = 0.88,
                reason = "pago parcial",
                routerSource = "woz",
                entities = mapOf("amount" to "50", "cardAlias" to "oro"),
            ),
        )

        val history = turns.toHistoryMessages()

        assertEquals(4, history.size)
        assertEquals(ChatHistoryRole.USER, history[0].role)
        assertEquals("cuanto debo en la oro", history[0].content)
        assertEquals(ChatHistoryRole.ASSISTANT, history[1].role)
        assertTrue(history[1].content.contains("\"intent\":\"CHECK_BALANCE\""))
        assertTrue(history[1].content.contains("\"cardAlias\":\"oro\""))
        assertTrue(history[1].content.contains("\"confidence\":0.91"))
        assertEquals(ChatHistoryRole.USER, history[2].role)
        assertEquals("paga 50", history[2].content)
        assertTrue(history[3].content.contains("\"amount\":\"50\""))
    }
}
