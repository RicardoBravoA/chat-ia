package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.sdui.UiNode

class NotImplementedTransferUiBuilder : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode {
        val label = when (ctx.classification.intent) {
            IntentLabel.TRANSFER_OWN_ACCOUNTS -> "transferencias entre tus cuentas"
            IntentLabel.TRANSFER_THIRD_PARTY -> "transferencias a terceros"
            else -> "esta operación"
        }
        return SduiNodeFactory.column(
            "transfer-unavailable",
            SduiNodeFactory.infoBanner(
                "transfer-banner",
                "Próximamente: $label estará disponible en la app.",
            ),
            SduiNodeFactory.assistantText(
                "transfer-text",
                "Por ahora puedo ayudarte con consulta de saldo y pago de tarjeta de crédito.",
            ),
        )
    }
}
