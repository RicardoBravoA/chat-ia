package com.bank.banking.api.intent

data class WozConfig(
    val ollamaBaseUrl: String,
    val model: String,
    val timeoutSeconds: Long,
    val minConfidenceForAccept: Double,
) {
    companion object {
        fun fromEnvironment(): WozConfig {
            val baseUrl = (System.getenv("WOZ_OLLAMA_BASE_URL") ?: "http://127.0.0.1:11434")
                .trim()
                .trimEnd('/')
            val model = (System.getenv("WOZ_MODEL") ?: "qwen2.5:7b-instruct").trim()
            val timeout = System.getenv("WOZ_TIMEOUT_SECONDS")
                ?.trim()
                ?.toLongOrNull()
                ?.coerceIn(5L, 300L)
                ?: 60L
            val minConfidence = System.getenv("WOZ_MIN_CONFIDENCE_FOR_ACCEPT")
                ?.trim()
                ?.toDoubleOrNull()
                ?.coerceIn(0.0, 1.0)
                ?: 0.55
            return WozConfig(
                ollamaBaseUrl = baseUrl,
                model = model,
                timeoutSeconds = timeout,
                minConfidenceForAccept = minConfidence,
            )
        }
    }
}
