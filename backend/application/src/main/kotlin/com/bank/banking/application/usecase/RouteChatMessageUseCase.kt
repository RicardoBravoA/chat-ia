package com.bank.banking.application.usecase

import com.bank.banking.application.BackendRouteResolver
import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.model.BackendRouteHint
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.port.IntentClassifierPort

data class ChatRouteResult(
    val userMessage: String,
    val classification: IntentClassification,
    val suggestedActions: List<BackendRouteHint>,
)

class RouteChatMessageUseCase(
    private val intentClassifier: IntentClassifierPort,
) {
    suspend fun execute(message: String): ChatRouteResult {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            throw BadRequestException("message must not be blank")
        }
        if (trimmed.length > 4_000) {
            throw BadRequestException("message too long")
        }
        val classification = intentClassifier.classify(trimmed)
        val suggested = BackendRouteResolver.resolve(classification)
        return ChatRouteResult(
            userMessage = trimmed,
            classification = classification,
            suggestedActions = suggested,
        )
    }
}
