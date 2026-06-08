package com.bank.banking.domain.model

data class ChatSessionId(val value: String)

data class ChatTurn(
    val userMessage: String,
    val intent: String?,
    val correlationId: String?,
    val createdAtEpochMs: Long,
    val confidence: Double? = null,
    val reason: String? = null,
    val routerSource: String? = null,
    val entities: Map<String, String> = emptyMap(),
)

data class ChatSession(
    val id: ChatSessionId,
    val userId: UserId,
    val turns: List<ChatTurn>,
    val updatedAtEpochMs: Long,
)

enum class ChatHistoryRole(val wireValue: String) {
    USER("user"),
    ASSISTANT("assistant"),
}

data class ChatHistoryMessage(
    val role: ChatHistoryRole,
    val content: String,
)

fun List<ChatTurn>.toHistoryMessages(): List<ChatHistoryMessage> =
    flatMap { turn ->
        listOf(
            ChatHistoryMessage(ChatHistoryRole.USER, turn.userMessage),
            ChatHistoryMessage(ChatHistoryRole.ASSISTANT, turn.toAssistantHistoryContent()),
        )
    }

private fun ChatTurn.toAssistantHistoryContent(): String =
    buildString {
        append("{")
        append("\"intent\":\"${intent ?: "UNKNOWN"}\"")
        confidence?.let { append(",\"confidence\":$it") }
        if (entities.isNotEmpty()) {
            append(",\"entities\":{")
            append(
                entities.entries.joinToString(",") { (key, value) ->
                    "\"${key.escapeJson()}\":\"${value.escapeJson()}\""
                },
            )
            append("}")
        }
        reason?.takeIf { it.isNotBlank() }?.let {
            append(",\"reason\":\"${it.escapeJson()}\"")
        }
        routerSource?.takeIf { it.isNotBlank() }?.let {
            append(",\"source\":\"${it.escapeJson()}\"")
        }
        correlationId?.takeIf { it.isNotBlank() }?.let {
            append(",\"correlationId\":\"${it.escapeJson()}\"")
        }
        append("}")
    }

private fun String.escapeJson(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
