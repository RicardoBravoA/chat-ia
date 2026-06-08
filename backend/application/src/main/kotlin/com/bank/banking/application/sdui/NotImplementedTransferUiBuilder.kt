package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.sdui.UiNode

class NotImplementedTransferUiBuilder : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode {
        val label = when (ctx.classification.intent) {
            IntentLabel.TRANSFER_OWN_ACCOUNTS -> "las transferencias entre tus cuentas"
            IntentLabel.TRANSFER_THIRD_PARTY -> "las transferencias a terceros"
            else -> "esta operación"
        }
        return SduiNodeFactory.column(
            "transfer-unavailable",
            SduiNodeFactory.assistantText(
                "transfer-text",
                GroundedChatCopy.transferUnavailable(label),
            ),
            SduiNodeFactory.infoBanner(
                "transfer-banner",
                "Próximamente podrás completar $label desde el chat.",
            ),
        )
    }
}
