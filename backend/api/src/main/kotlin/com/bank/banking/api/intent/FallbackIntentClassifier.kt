package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.port.IntentClassifierPort

/**
 * Intenta el clasificador primario (p. ej. Python); si falla, usa el respaldo heurístico.
 */
class FallbackIntentClassifier(
    private val primary: IntentClassifierPort,
    private val fallback: IntentClassifierPort,
    private val fallbackOnResult: (IntentClassification) -> Boolean = { false },
) : IntentClassifierPort {
    override suspend fun classify(message: String): IntentClassification {
        return try {
            val primaryResult = primary.classify(message)
            if (fallbackOnResult(primaryResult)) {
                fallback.classify(message)
            } else {
                primaryResult
            }
        } catch (_: Exception) {
            fallback.classify(message)
        }
    }
}
