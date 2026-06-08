package com.bank.banking.api

import com.bank.banking.api.dto.ChatMessageResponse
import com.bank.banking.api.dto.UiActionDto
import com.bank.banking.api.dto.UiMetadataDto
import com.bank.banking.api.dto.UiNodeDto
import com.bank.banking.domain.model.sdui.UiAction
import com.bank.banking.domain.model.sdui.UiMetadata
import com.bank.banking.domain.model.sdui.UiNode
import com.bank.banking.domain.model.sdui.UiResponse

internal fun UiAction.toDto(): UiActionDto =
    UiActionDto(
        id = id,
        label = label,
        actionType = actionType,
        payload = payload,
        requiresConfirmation = requiresConfirmation,
    )

internal fun UiNode.toDto(): UiNodeDto =
    UiNodeDto(
        id = id,
        type = type,
        props = props,
        children = children.map { it.toDto() },
        actions = actions.map { it.toDto() },
    )

internal fun UiMetadata.toDto(): UiMetadataDto =
    UiMetadataDto(
        intent = intent,
        confidence = confidence,
        clarificationNeeded = clarificationNeeded,
        reason = reason,
        routerSource = routerSource,
    )

internal fun UiResponse.toResponse(): ChatMessageResponse =
    ChatMessageResponse(
        schemaVersion = schemaVersion,
        correlationId = correlationId,
        sessionId = sessionId,
        userMessage = userMessage,
        uiTree = uiTree.toDto(),
        metadata = metadata.toDto(),
    )
