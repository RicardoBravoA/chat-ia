package com.bank.banking.api.intent

import com.bank.banking.domain.port.IntentClassifierPort

object IntentClassifierFactory {
    /**
     * - [INTENT_ROUTER_MODE]: `auto` (defecto), `woz`, `heuristic`.
     * - [WOZ_OLLAMA_BASE_URL]: base de Ollama (default `http://127.0.0.1:11434`).
     * - [WOZ_MODEL]: modelo Ollama (default `qwen2.5:7b-instruct`).
     * - [WOZ_TIMEOUT_SECONDS]: timeout HTTP (default 60).
     * - [WOZ_MIN_CONFIDENCE_FOR_ACCEPT]: umbral de aceptación Woz en auto (default 0.55).
     * - [INTENT_HEURISTIC_FAST_PATH_MIN_CONFIDENCE]: fast path heurístico en auto (default 0.85).
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
                CascadeIntentClassifier(
                    heuristic = heuristic,
                    woz = woz,
                    heuristicFastPathMinConfidence = HeuristicFastPathPolicy.minConfidenceFromEnvironment(),
                    wozMinConfidenceForAccept = wozConfig.minConfidenceForAccept,
                )
        }
    }
}
