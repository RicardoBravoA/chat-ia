package com.bank.banking.api.intent

import com.bank.banking.domain.model.ChatHistoryMessage
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.port.IntentClassifierPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clasificador de intenciones vía **Woz** — LLM local servido por Ollama.
 *
 * Variables: [WozConfig] (`WOZ_OLLAMA_BASE_URL`, `WOZ_MODEL`, `WOZ_TIMEOUT_SECONDS`).
 */
class WozIntentClassifier(
    private val config: WozConfig,
    private val ollamaClient: WozOllamaClient = WozOllamaClient(config),
) : IntentClassifierPort {
    override suspend fun classify(
        message: String,
        history: List<ChatHistoryMessage>,
    ): IntentClassification =
        withContext(Dispatchers.IO) {
            val content = ollamaClient.classifyUserMessage(message.trim(), history)
            WozResponseParser.parseModelContent(content)
        }
}
