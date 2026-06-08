package com.bank.mobile.domain.model.sdui

import kotlinx.serialization.Serializable

@Serializable
data class UiAction(
    val id: String,
    val label: String,
    val actionType: String,
    val payload: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
)

@Serializable
data class UiNode(
    val id: String,
    val type: String,
    val props: Map<String, String> = emptyMap(),
    val children: List<UiNode> = emptyList(),
    val actions: List<UiAction> = emptyList(),
)

@Serializable
data class UiMetadata(
    val intent: String,
    val confidence: Double,
    val clarificationNeeded: Boolean,
    val reason: String,
    val routerSource: String,
)

@Serializable
data class ChatUiResponse(
    val schemaVersion: Int,
    val correlationId: String,
    val sessionId: String,
    val userMessage: String,
    val uiTree: UiNode,
    val metadata: UiMetadata,
)
