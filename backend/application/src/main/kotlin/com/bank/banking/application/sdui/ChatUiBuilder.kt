package com.bank.banking.application.sdui

import com.bank.banking.domain.model.sdui.UiNode

fun interface ChatUiBuilder {
    suspend fun build(ctx: ChatUiBuildContext): UiNode
}
