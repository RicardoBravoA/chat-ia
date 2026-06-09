package com.bank.banking.api.intent

import com.bank.banking.domain.model.ChatHistoryMessage
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.port.IntentClassifierPort
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CascadeIntentClassifierTest {
    @Test
    fun `uses heuristic fast path when match is satisfactory`() = runBlocking {
        val heuristic = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.CHECK_BALANCE,
                confidence = 0.92,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "rule",
                source = "heuristic",
            ),
        )
        val woz = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.2,
                entities = emptyMap(),
                clarificationNeeded = true,
                reason = "woz",
                source = "woz",
            ),
        )
        val classifier = CascadeIntentClassifier(
            heuristic = heuristic,
            woz = woz,
            heuristicFastPathMinConfidence = 0.85,
            wozMinConfidenceForAccept = 0.55,
        )

        val result = classifier.classify("ver mi saldo", emptyList())

        assertEquals(IntentLabel.CHECK_BALANCE, result.intent)
        assertEquals("heuristic", result.source)
        assertEquals(1, heuristic.callCount)
        assertEquals(0, woz.callCount)
    }

    @Test
    fun `calls woz when heuristic is not satisfactory`() = runBlocking {
        val heuristic = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.75,
                entities = emptyMap(),
                clarificationNeeded = true,
                reason = "no rule",
                source = "heuristic",
            ),
        )
        val woz = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.PAY_CREDIT_CARD,
                confidence = 0.91,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "woz",
                source = "woz",
            ),
        )
        val classifier = CascadeIntentClassifier(
            heuristic = heuristic,
            woz = woz,
            heuristicFastPathMinConfidence = 0.85,
            wozMinConfidenceForAccept = 0.55,
        )

        val result = classifier.classify("paga 50", emptyList())

        assertEquals(IntentLabel.PAY_CREDIT_CARD, result.intent)
        assertEquals("woz", result.source)
        assertEquals(1, heuristic.callCount)
        assertEquals(1, woz.callCount)
    }

    @Test
    fun `falls back to heuristic when woz fails technically`() = runBlocking {
        val heuristic = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.75,
                entities = emptyMap(),
                clarificationNeeded = true,
                reason = "no rule",
                source = "heuristic",
            ),
            secondResult = IntentClassification(
                intent = IntentLabel.PAY_CREDIT_CARD,
                confidence = 0.9,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "rule",
                source = "heuristic",
            ),
        )
        val woz = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.PAY_CREDIT_CARD,
                confidence = 0.91,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "woz",
                source = "woz",
            ),
            throwsOnCall = true,
        )
        val classifier = CascadeIntentClassifier(
            heuristic = heuristic,
            woz = woz,
            heuristicFastPathMinConfidence = 0.85,
            wozMinConfidenceForAccept = 0.55,
        )

        val result = classifier.classify("pagar mi tc", emptyList())

        assertEquals(IntentLabel.PAY_CREDIT_CARD, result.intent)
        assertEquals("heuristic", result.source)
        assertEquals(2, heuristic.callCount)
        assertEquals(1, woz.callCount)
    }

    @Test
    fun `falls back to heuristic when woz result is not acceptable`() = runBlocking {
        val heuristic = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.75,
                entities = emptyMap(),
                clarificationNeeded = true,
                reason = "no rule",
                source = "heuristic",
            ),
            secondResult = IntentClassification(
                intent = IntentLabel.CHECK_BALANCE,
                confidence = 0.92,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "rule",
                source = "heuristic",
            ),
        )
        val woz = RecordingClassifier(
            IntentClassification(
                intent = IntentLabel.AMBIGUOUS,
                confidence = 0.4,
                entities = emptyMap(),
                clarificationNeeded = true,
                reason = "unsure",
                source = "woz",
            ),
        )
        val classifier = CascadeIntentClassifier(
            heuristic = heuristic,
            woz = woz,
            heuristicFastPathMinConfidence = 0.85,
            wozMinConfidenceForAccept = 0.55,
        )

        val result = classifier.classify("algo raro", emptyList())

        assertEquals(IntentLabel.CHECK_BALANCE, result.intent)
        assertEquals("heuristic", result.source)
        assertEquals(2, heuristic.callCount)
        assertEquals(1, woz.callCount)
    }

    @Test
    fun `shouldEscalateFromWozToHeuristic detects low confidence and ambiguous`() {
        val lowConfidence = IntentClassification(
            intent = IntentLabel.CHECK_BALANCE,
            confidence = 0.4,
            entities = emptyMap(),
            clarificationNeeded = false,
            reason = "woz",
            source = "woz",
        )
        val ambiguous = IntentClassification(
            intent = IntentLabel.AMBIGUOUS,
            confidence = 0.9,
            entities = emptyMap(),
            clarificationNeeded = true,
            reason = "woz",
            source = "woz",
        )
        val acceptable = IntentClassification(
            intent = IntentLabel.CHECK_BALANCE,
            confidence = 0.9,
            entities = emptyMap(),
            clarificationNeeded = false,
            reason = "woz",
            source = "woz",
        )

        assertTrue(CascadeIntentClassifier.shouldEscalateFromWozToHeuristic(lowConfidence, 0.55))
        assertTrue(CascadeIntentClassifier.shouldEscalateFromWozToHeuristic(ambiguous, 0.55))
        assertFalse(CascadeIntentClassifier.shouldEscalateFromWozToHeuristic(acceptable, 0.55))
    }

    private class RecordingClassifier(
        private val firstResult: IntentClassification,
        private val secondResult: IntentClassification? = null,
        private val throwsOnCall: Boolean = false,
    ) : IntentClassifierPort {
        var callCount: Int = 0

        override suspend fun classify(
            message: String,
            history: List<ChatHistoryMessage>,
        ): IntentClassification {
            callCount++
            if (throwsOnCall) {
                throw IllegalStateException("woz unavailable")
            }
            return if (callCount == 1 || secondResult == null) {
                firstResult
            } else {
                secondResult
            }
        }
    }
}
