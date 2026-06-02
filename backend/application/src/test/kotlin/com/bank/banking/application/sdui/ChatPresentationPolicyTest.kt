package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatPresentationPolicyTest {
    @Test
    fun `ambiguous intent asks clarification`() {
        val c = classification(intent = IntentLabel.AMBIGUOUS, confidence = 0.99)
        assertTrue(ChatPresentationPolicy.shouldAskClarification(c))
    }

    @Test
    fun `low confidence asks clarification`() {
        val c = classification(intent = IntentLabel.CHECK_BALANCE, confidence = 0.4)
        assertTrue(ChatPresentationPolicy.shouldAskClarification(c))
    }

    @Test
    fun `high confidence check balance proceeds`() {
        val c = classification(intent = IntentLabel.CHECK_BALANCE, confidence = 0.9)
        assertFalse(ChatPresentationPolicy.shouldAskClarification(c))
    }

    @Test
    fun `out of scope high confidence shows support`() {
        val c = classification(intent = IntentLabel.OUT_OF_SCOPE, confidence = 0.85)
        assertTrue(ChatPresentationPolicy.shouldShowSupport(c, "clima hoy"))
    }

    private fun classification(intent: IntentLabel, confidence: Double) =
        IntentClassification(
            intent = intent,
            confidence = confidence,
            entities = emptyMap(),
            clarificationNeeded = false,
            reason = "test",
            source = "test",
        )
}
