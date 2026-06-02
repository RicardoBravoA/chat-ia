package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel

object ChatPresentationPolicy {
    private const val LOW_CONFIDENCE_THRESHOLD = 0.55
    private const val OUT_OF_SCOPE_HIGH_CONFIDENCE_THRESHOLD = 0.8

    fun shouldAskClarification(classification: IntentClassification): Boolean =
        classification.clarificationNeeded ||
            classification.intent == IntentLabel.AMBIGUOUS ||
            classification.confidence < LOW_CONFIDENCE_THRESHOLD

    fun shouldShowSupport(classification: IntentClassification, userMessage: String): Boolean {
        if (classification.intent != IntentLabel.OUT_OF_SCOPE) return false
        if (classification.confidence >= OUT_OF_SCOPE_HIGH_CONFIDENCE_THRESHOLD) return true
        return !appearsBankingRelated(userMessage)
    }

    fun shouldRecheckOutOfScope(classification: IntentClassification, userMessage: String): Boolean =
        classification.intent == IntentLabel.OUT_OF_SCOPE &&
            classification.confidence < OUT_OF_SCOPE_HIGH_CONFIDENCE_THRESHOLD &&
            appearsBankingRelated(userMessage)

    private fun appearsBankingRelated(text: String): Boolean {
        val normalized = text.lowercase()
        val keywords = listOf(
            "saldo", "pagar", "pago", "tarjeta", "transfer", "cuenta", "movimientos", "deuda",
        )
        return keywords.any { it in normalized }
    }
}
