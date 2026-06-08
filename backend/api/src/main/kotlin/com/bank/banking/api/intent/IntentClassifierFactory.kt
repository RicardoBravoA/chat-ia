package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.port.IntentClassifierPort

object IntentClassifierFactory {
    /**
     * - [INTENT_ROUTER_MODE]: `auto` (defecto), `woz`, `heuristic`.
     * - [WOZ_OLLAMA_BASE_URL]: base de Ollama (default `http://127.0.0.1:11434`).
     * - [WOZ_MODEL]: modelo Ollama (default `qwen2.5:7b-instruct`).
     * - [WOZ_TIMEOUT_SECONDS]: timeout HTTP (default 60).
     * - [WOZ_MIN_CONFIDENCE_FOR_ACCEPT]: umbral de aceptación Woz en auto (default 0.55).
     */
    fun createFromEnvironment(): IntentClassifierPort {
        val heuristic = HeuristicIntentClassifier(LocalIntentHeuristicLoader.load())
        val mode = (System.getenv("INTENT_ROUTER_MODE") ?: "auto").lowercase()
        val wozConfig = WozConfig.fromEnvironment()
        val woz = WozIntentClassifier(wozConfig)

        return when (mode) {
            "heuristic" -> heuristic
            "woz" -> woz
            else ->
                FallbackIntentClassifier(
                    primary = woz,
                    fallback = heuristic,
                    fallbackOnResult = { wozResult ->
                        shouldEscalateToFallback(wozResult, wozConfig.minConfidenceForAccept)
                    },
                )
        }
    }
}

private fun shouldEscalateToFallback(
    wozResult: IntentClassification,
    minConfidence: Double,
): Boolean {
    return wozResult.clarificationNeeded ||
        wozResult.intent == IntentLabel.AMBIGUOUS ||
        wozResult.confidence < minConfidence
}
