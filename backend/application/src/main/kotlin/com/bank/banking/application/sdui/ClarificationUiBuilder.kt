package com.bank.banking.application.sdui

import com.bank.banking.domain.model.sdui.UiNode

class ClarificationUiBuilder : ChatUiBuilder {
    override suspend fun build(ctx: ChatUiBuildContext): UiNode =
        SduiNodeFactory.column(
            "clarification-root",
            SduiNodeFactory.assistantText("clarification-text", CLARIFICATION_MESSAGE),
        )

    companion object {
        const val CLARIFICATION_MESSAGE =
            "No tengo claro qué operación necesitas. ¿Quieres consultar tu saldo, pagar tu tarjeta de crédito " +
                "o hacer una transferencia? Describe en una frase lo que buscas y te guío."
    }
}
