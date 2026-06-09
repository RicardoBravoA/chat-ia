package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel

/**
 * Criterio para aceptar la heurística en el **fast path** del modo `auto`
 * sin invocar Woz (Ollama).
 */
object HeuristicFastPathPolicy {
    fun isSatisfactory(
        result: IntentClassification,
        minConfidence: Double,
    ): Boolean =
        result.intent != IntentLabel.AMBIGUOUS &&
            !result.clarificationNeeded &&
            result.confidence >= minConfidence

    fun minConfidenceFromEnvironment(): Double =
        System.getenv("INTENT_HEURISTIC_FAST_PATH_MIN_CONFIDENCE")
            ?.trim()
            ?.toDoubleOrNull()
            ?.coerceIn(0.0, 1.0)
            ?: DEFAULT_MIN_CONFIDENCE

    const val DEFAULT_MIN_CONFIDENCE = 0.85
}
