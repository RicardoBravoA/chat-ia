package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

object WozResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class WozModelPayload(
        val intent: String = "AMBIGUOUS",
        val confidence: Double = 0.0,
        @SerialName("clarification_needed") val clarificationNeeded: Boolean = true,
        val reason: String = "",
        val entities: Map<String, JsonElement> = emptyMap(),
    )

    fun parseModelContent(content: String): IntentClassification {
        val payload = json.decodeFromString<WozModelPayload>(extractJsonObject(content))
        val intent = IntentLabel.fromClassifierOutput(payload.intent)
        val confidence = payload.confidence.coerceIn(0.0, 1.0)
        val entities = payload.entities.mapValues { (_, value) -> jsonElementToString(value) }
        val clarificationNeeded =
            payload.clarificationNeeded ||
                intent == IntentLabel.AMBIGUOUS ||
                confidence < 0.55
        return IntentClassification(
            intent = intent,
            confidence = confidence,
            entities = entities,
            clarificationNeeded = clarificationNeeded,
            reason = payload.reason.ifBlank { "Woz local LLM classification" },
            source = "woz",
        )
    }

    private fun extractJsonObject(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object in Woz response: ${text.take(300)}" }
        return text.substring(start, end + 1)
    }

    private fun jsonElementToString(element: JsonElement): String =
        when (element) {
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
}
