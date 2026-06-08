package com.bank.banking.application.sdui

import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.model.IntentLabel

object ChatQuickReplyPolicy {
    private val allowedIntents: Set<IntentLabel> =
        GreetingUiBuilder.QUICK_REPLIES.map { it.intent }.toSet()

    fun parseAllowedQuickReplyIntent(raw: String?): IntentLabel? {
        if (raw.isNullOrBlank()) return null
        val intent = IntentLabel.fromClassifierOutput(raw.trim())
        return intent.takeIf { it in allowedIntents }
    }

    fun requireAllowedQuickReplyIntent(raw: String): IntentLabel =
        parseAllowedQuickReplyIntent(raw)
            ?: throw BadRequestException("selectedIntent is not an allowed server quick reply")
}
