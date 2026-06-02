package com.bank.banking.application.sdui

import com.bank.banking.domain.model.sdui.UiNode

class SupportUiBuilder : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode =
        SduiNodeFactory.column(
            "support-root",
            SduiNodeFactory.supportChannelsCard("support-card"),
        )
}
