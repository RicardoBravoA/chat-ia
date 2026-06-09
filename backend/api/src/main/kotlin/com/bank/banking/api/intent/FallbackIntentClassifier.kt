package com.bank.banking.api.intent

import com.bank.banking.domain.model.ChatHistoryMessage
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.port.IntentClassifierPort

/**
 * Intenta el clasificador primario; si falla o el resultado no es aceptable, usa el respaldo.
 *
 * Modo `auto` usa [CascadeIntentClassifier] (heurística → Woz → heurística); esta clase queda
 * disponible para composiciones puntuales o tests.
 */
class FallbackIntentClassifier(
    private val primary: IntentClassifierPort,
    private val fallback: IntentClassifierPort,
    private val fallbackOnResult: (IntentClassification) -> Boolean = { false },
) : IntentClassifierPort {
    override suspend fun classify(
        message: String,
        history: List<ChatHistoryMessage>,
    ): IntentClassification {
        return try {
            val primaryResult = primary.classify(message, history)
            if (fallbackOnResult(primaryResult)) {
                fallback.classify(message, history)
            } else {
                primaryResult
            }
        } catch (_: Exception) {
            fallback.classify(message, history)
        }
    }
}
