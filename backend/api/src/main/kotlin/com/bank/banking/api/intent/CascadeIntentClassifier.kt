package com.bank.banking.api.intent

import com.bank.banking.domain.model.ChatHistoryMessage
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.port.IntentClassifierPort

/**
 * Router en cascada para modo `auto`:
 *
 * 1. **Heurística** — si el match es satisfactorio (fast path), devuelve sin llamar a Ollama.
 * 2. **Woz (LLM)** — si la heurística no resolvió con confianza suficiente.
 * 3. **Heurística otra vez** — si Woz falla técnicamente o su resultado no es aceptable.
 */
class CascadeIntentClassifier(
    private val heuristic: IntentClassifierPort,
    private val woz: IntentClassifierPort,
    private val heuristicFastPathMinConfidence: Double = HeuristicFastPathPolicy.DEFAULT_MIN_CONFIDENCE,
    private val wozMinConfidenceForAccept: Double,
) : IntentClassifierPort {
    override suspend fun classify(
        message: String,
        history: List<ChatHistoryMessage>,
    ): IntentClassification {
        val heuristicResult = heuristic.classify(message, history)
        if (HeuristicFastPathPolicy.isSatisfactory(heuristicResult, heuristicFastPathMinConfidence)) {
            return heuristicResult
        }

        return try {
            val wozResult = woz.classify(message, history)
            if (shouldEscalateFromWozToHeuristic(wozResult, wozMinConfidenceForAccept)) {
                heuristic.classify(message, history)
            } else {
                wozResult
            }
        } catch (_: Exception) {
            heuristic.classify(message, history)
        }
    }

    companion object {
        internal fun shouldEscalateFromWozToHeuristic(
            wozResult: IntentClassification,
            minConfidence: Double,
        ): Boolean =
            wozResult.clarificationNeeded ||
                wozResult.intent == IntentLabel.AMBIGUOUS ||
                wozResult.confidence < minConfidence
    }
}
