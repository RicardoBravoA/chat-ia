package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeuristicFastPathPolicyTest {
    @Test
    fun `is satisfactory when intent is clear and confidence meets threshold`() {
        val result = IntentClassification(
            intent = IntentLabel.CHECK_BALANCE,
            confidence = 0.92,
            entities = emptyMap(),
            clarificationNeeded = false,
            reason = "rule",
            source = "heuristic",
        )

        assertTrue(HeuristicFastPathPolicy.isSatisfactory(result, 0.85))
    }

    @Test
    fun `is not satisfactory when ambiguous or clarification needed`() {
        val ambiguous = IntentClassification(
            intent = IntentLabel.AMBIGUOUS,
            confidence = 0.75,
            entities = emptyMap(),
            clarificationNeeded = true,
            reason = "no rule",
            source = "heuristic",
        )
        val lowConfidence = IntentClassification(
            intent = IntentLabel.CHECK_BALANCE,
            confidence = 0.7,
            entities = emptyMap(),
            clarificationNeeded = true,
            reason = "low",
            source = "heuristic",
        )

        assertFalse(HeuristicFastPathPolicy.isSatisfactory(ambiguous, 0.85))
        assertFalse(HeuristicFastPathPolicy.isSatisfactory(lowConfidence, 0.85))
    }
}
