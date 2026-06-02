package com.bank.mobile.data.mapper

import com.bank.mobile.data.remote.dto.ChatMessageResponseDto
import com.bank.mobile.data.remote.dto.UiActionDto
import com.bank.mobile.data.remote.dto.UiMetadataDto
import com.bank.mobile.data.remote.dto.UiNodeDto
import com.bank.mobile.domain.model.sdui.ChatUiResponse
import com.bank.mobile.domain.model.sdui.UiAction
import com.bank.mobile.domain.model.sdui.UiMetadata
import com.bank.mobile.domain.model.sdui.UiNode

fun ChatMessageResponseDto.toDomain(): ChatUiResponse =
    ChatUiResponse(
        schemaVersion = schemaVersion,
        correlationId = correlationId,
        userMessage = userMessage,
        uiTree = uiTree.toDomain(),
        metadata = metadata.toDomain(),
    )

private fun UiMetadataDto.toDomain(): UiMetadata =
    UiMetadata(
        intent = intent,
        confidence = confidence,
        clarificationNeeded = clarificationNeeded,
        reason = reason,
        routerSource = routerSource,
    )

private fun UiNodeDto.toDomain(): UiNode =
    UiNode(
        id = id,
        type = type,
        props = props,
        children = children.map { it.toDomain() },
        actions = actions.map { it.toDomain() },
    )

private fun UiActionDto.toDomain(): UiAction =
    UiAction(
        id = id,
        label = label,
        actionType = actionType,
        payload = payload,
        requiresConfirmation = requiresConfirmation,
    )
