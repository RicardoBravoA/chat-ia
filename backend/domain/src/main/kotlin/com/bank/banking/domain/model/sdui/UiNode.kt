package com.bank.banking.domain.model.sdui

data class UiAction(
    val id: String,
    val label: String,
    val actionType: String,
    val payload: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
)

data class UiNode(
    val id: String,
    val type: String,
    val props: Map<String, String> = emptyMap(),
    val children: List<UiNode> = emptyList(),
    val actions: List<UiAction> = emptyList(),
)

data class UiMetadata(
    val intent: String,
    val confidence: Double,
    val clarificationNeeded: Boolean,
    val reason: String,
    val routerSource: String,
)

data class UiResponse(
    val schemaVersion: Int,
    val correlationId: String,
    val userMessage: String,
    val uiTree: UiNode,
    val metadata: UiMetadata,
)
