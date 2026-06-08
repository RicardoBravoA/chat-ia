package com.bank.banking.api.intent

import com.bank.banking.domain.model.ChatHistoryMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class WozOllamaClient(
    private val config: WozConfig,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val stream: Boolean = false,
        val format: String = "json",
        val options: ChatOptions = ChatOptions(),
    )

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String,
    )

    @Serializable
    private data class ChatOptions(
        val temperature: Double = 0.0,
    )

    @Serializable
    private data class ChatResponse(
        val message: ChatResponseMessage,
    )

    @Serializable
    private data class ChatResponseMessage(
        val content: String,
    )

    fun classifyUserMessage(
        userText: String,
        history: List<ChatHistoryMessage> = emptyList(),
    ): String {
        val messages = buildList {
            add(ChatMessage(role = "system", content = WozPrompt.SYSTEM))
            history.forEach { turn ->
                add(ChatMessage(role = turn.role.wireValue, content = turn.content))
            }
            add(ChatMessage(role = "user", content = WozPrompt.userMessage(userText)))
        }
        val payload = ChatRequest(
            model = config.model,
            messages = messages,
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${config.ollamaBaseUrl}/api/chat"))
            .timeout(Duration.ofSeconds(config.timeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(payload)))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(
                "Woz Ollama HTTP ${response.statusCode()}: ${response.body().take(500)}",
            )
        }
        val body = json.decodeFromString<ChatResponse>(response.body())
        return body.message.content
    }
}
