package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentLabel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HeuristicIntentClassifierTest {
    private val classifier = HeuristicIntentClassifier(LocalIntentHeuristicLoader.load())

    @Test
    fun `classifies greeting plus pay gerund as pay credit card`() = runBlocking {
        val result = classifier.classify("hola, puedes ayudarme pagando mi tc", emptyList())

        assertEquals(IntentLabel.PAY_CREDIT_CARD, result.intent)
        assertFalse(result.clarificationNeeded)
        assertEquals("heuristic", result.source)
    }
}
