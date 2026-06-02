package com.bank.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageRequestDto(val message: String)

@Serializable
data class UiActionDto(
    val id: String,
    val label: String,
    val actionType: String,
    val payload: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
)

@Serializable
data class UiNodeDto(
    val id: String,
    val type: String,
    val props: Map<String, String> = emptyMap(),
    val children: List<UiNodeDto> = emptyList(),
    val actions: List<UiActionDto> = emptyList(),
)

@Serializable
data class UiMetadataDto(
    val intent: String,
    val confidence: Double,
    val clarificationNeeded: Boolean,
    val reason: String,
    val routerSource: String,
)

@Serializable
data class ChatMessageResponseDto(
    val schemaVersion: Int,
    val correlationId: String,
    val userMessage: String,
    val uiTree: UiNodeDto,
    val metadata: UiMetadataDto,
)

@Serializable
data class ErrorResponseDto(
    val code: String,
    val message: String,
)

@Serializable
data class ChatWsEnvelopeDto(
    val event: String,
    val response: ChatMessageResponseDto? = null,
    val error: ErrorResponseDto? = null,
)
