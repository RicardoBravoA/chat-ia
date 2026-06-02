package com.bank.banking.application.usecase

import com.bank.banking.application.sdui.ChatPresentationPolicy
import com.bank.banking.application.sdui.ChatUiBuildContext
import com.bank.banking.application.sdui.ChatUiBuilderFactory
import com.bank.banking.application.sdui.ClarificationUiBuilder
import com.bank.banking.application.sdui.SupportUiBuilder
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.sdui.UiMetadata
import com.bank.banking.domain.model.sdui.UiResponse
import java.util.UUID

class BuildChatUiUseCase(
    private val routeChatMessage: RouteChatMessageUseCase,
    private val builderFactory: ChatUiBuilderFactory,
    private val clarificationBuilder: ClarificationUiBuilder,
    private val supportBuilder: SupportUiBuilder,
) {
    suspend fun execute(token: SessionToken, message: String): UiResponse {
        val route = routeChatMessage.execute(message)
        val classification = route.classification
        val ctx = ChatUiBuildContext(
            token = token,
            userMessage = message,
            classification = classification,
        )
        val uiTree = when {
            ChatPresentationPolicy.shouldAskClarification(classification) ||
                ChatPresentationPolicy.shouldRecheckOutOfScope(classification, message) ->
                clarificationBuilder.build(ctx)
            ChatPresentationPolicy.shouldShowSupport(classification, message) ->
                supportBuilder.build(ctx)
            else -> builderFactory.forIntent(classification.intent).build(ctx)
        }
        return UiResponse(
            schemaVersion = SCHEMA_VERSION,
            correlationId = UUID.randomUUID().toString(),
            userMessage = route.userMessage,
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

    companion object {
        const val SCHEMA_VERSION = 1
    }
}
