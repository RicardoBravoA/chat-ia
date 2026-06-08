package com.bank.banking.application.usecase

import com.bank.banking.application.sdui.ChatPresentationPolicy
import com.bank.banking.application.sdui.ChatQuickReplyPolicy
import com.bank.banking.application.sdui.ChatUiBuildContext
import com.bank.banking.application.sdui.ChatUiBuilderFactory
import com.bank.banking.application.sdui.ClarificationUiBuilder
import com.bank.banking.application.sdui.GreetingUiBuilder
import com.bank.banking.application.sdui.SupportUiBuilder
import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.error.NotFoundException
import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.ChatSessionId
import com.bank.banking.domain.model.ChatTurn
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.sdui.UiMetadata
import com.bank.banking.domain.model.sdui.UiResponse
import com.bank.banking.domain.model.toHistoryMessages
import com.bank.banking.domain.port.ChatSessionRepository
import com.bank.banking.domain.port.SessionRepository
import java.util.UUID

class BuildChatUiUseCase(
    private val sessions: SessionRepository,
    private val chatSessions: ChatSessionRepository,
    private val routeChatMessage: RouteChatMessageUseCase,
    private val builderFactory: ChatUiBuilderFactory,
    private val clarificationBuilder: ClarificationUiBuilder,
    private val supportBuilder: SupportUiBuilder,
    private val greetingBuilder: GreetingUiBuilder,
) {
    suspend fun execute(
        token: SessionToken,
        message: String,
        sessionId: ChatSessionId? = null,
        selectedIntent: String? = null,
    ): UiResponse {
        val userId = sessions.findValid(token)?.userId
            ?: throw UnauthorizedException("Invalid or expired session")

        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) {
            throw BadRequestException("message must not be blank")
        }

        val resolvedSessionId = resolveSessionId(sessionId, userId)
        val priorTurns = chatSessions.listRecentTurns(resolvedSessionId, userId, MAX_HISTORY_TURNS)
        val history = priorTurns.toHistoryMessages()

        val trimmedSelectedIntent = selectedIntent?.trim()?.takeIf { it.isNotEmpty() }
        val quickReplyIntent = trimmedSelectedIntent?.let {
            ChatQuickReplyPolicy.requireAllowedQuickReplyIntent(it)
        }

        val classification = if (quickReplyIntent != null) {
            IntentClassification(
                intent = quickReplyIntent,
                confidence = 1.0,
                entities = emptyMap(),
                clarificationNeeded = false,
                reason = "Server quick reply selection",
                source = "quick_reply",
            )
        } else {
            routeChatMessage.execute(trimmedMessage, history).classification
        }

        val ctx = ChatUiBuildContext(
            token = token,
            userMessage = trimmedMessage,
            classification = classification,
        )
        val showGreeting = quickReplyIntent == null &&
            ChatPresentationPolicy.shouldShowGreeting(classification, trimmedMessage)
        val uiTree = when {
            showGreeting -> greetingBuilder.build(ctx)
            ChatPresentationPolicy.shouldAskClarification(classification) ||
                ChatPresentationPolicy.shouldRecheckOutOfScope(classification, trimmedMessage) ->
                clarificationBuilder.build(ctx)
            ChatPresentationPolicy.shouldShowSupport(classification, trimmedMessage) ->
                supportBuilder.build(ctx)
            else -> builderFactory.forIntent(classification.intent).build(ctx)
        }
        val correlationId = UUID.randomUUID().toString()
        val storedIntent = when {
            showGreeting -> "GREETING"
            quickReplyIntent != null -> quickReplyIntent.name
            else -> classification.intent.name
        }
        chatSessions.appendTurn(
            resolvedSessionId,
            userId,
            ChatTurn(
                userMessage = trimmedMessage,
                intent = storedIntent,
                correlationId = correlationId,
                createdAtEpochMs = System.currentTimeMillis(),
                confidence = classification.confidence,
                reason = classification.reason,
                routerSource = classification.source,
                entities = classification.entities,
            ),
        )
        return UiResponse(
            schemaVersion = SCHEMA_VERSION,
            correlationId = correlationId,
            sessionId = resolvedSessionId.value,
            userMessage = trimmedMessage,
            uiTree = uiTree,
            metadata = UiMetadata(
                intent = classification.intent.name,
                confidence = classification.confidence,
                clarificationNeeded = classification.clarificationNeeded,
                reason = classification.reason,
                routerSource = classification.source,
            ),
        )
    }

    private suspend fun resolveSessionId(sessionId: ChatSessionId?, userId: com.bank.banking.domain.model.UserId): ChatSessionId {
        if (sessionId == null) {
            return chatSessions.create(userId)
        }
        chatSessions.findForUser(sessionId, userId)
            ?: throw NotFoundException("chat session not found")
        return sessionId
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_HISTORY_TURNS = 6
    }
}
