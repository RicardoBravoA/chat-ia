package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WozResponseParserTest {
    @Test
    fun `parses valid json content`() {
        val raw = """
            {"intent":"PAY_CREDIT_CARD","confidence":0.91,"entities":{"cardAlias":"oro"},
            "clarification_needed":false,"reason":"pago tarjeta"}
        """.trimIndent()
        val result = WozResponseParser.parseModelContent(raw)
        assertEquals(IntentLabel.PAY_CREDIT_CARD, result.intent)
        assertEquals(0.91, result.confidence)
        assertEquals("oro", result.entities["cardAlias"])
        assertEquals(false, result.clarificationNeeded)
        assertEquals("woz", result.source)
    }

    @Test
    fun `extracts json from prose wrapper`() {
        val raw = """Here is the result: {"intent":"CHECK_BALANCE","confidence":0.88,"entities":{},"clarification_needed":false,"reason":"saldo"}"""
        val result = WozResponseParser.parseModelContent(raw)
        assertEquals(IntentLabel.CHECK_BALANCE, result.intent)
    }

    @Test
    fun `unknown intent becomes ambiguous`() {
        val raw = """{"intent":"PAY_BILLS","confidence":0.5,"entities":{},"clarification_needed":false,"reason":"x"}"""
        val result = WozResponseParser.parseModelContent(raw)
        assertEquals(IntentLabel.AMBIGUOUS, result.intent)
        assertTrue(result.clarificationNeeded)
    }
}
