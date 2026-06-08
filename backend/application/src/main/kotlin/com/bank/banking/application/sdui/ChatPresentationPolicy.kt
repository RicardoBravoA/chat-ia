package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel

object ChatPresentationPolicy {
    private const val LOW_CONFIDENCE_THRESHOLD = 0.55
    private const val OUT_OF_SCOPE_HIGH_CONFIDENCE_THRESHOLD = 0.8

    private val greetingPhrases = listOf(
        "hola",
        "holaa",
        "holaaa",
        "buenos dias",
        "buenos días",
        "buenas tardes",
        "buenas noches",
        "buen dia",
        "buen día",
        "hey",
        "que tal",
        "qué tal",
        "saludos",
        "ayuda",
        "help",
        "que puedes hacer",
        "qué puedes hacer",
        "como funciona",
        "cómo funciona",
        "empezar",
        "inicio",
        "menu",
        "menú",
    )

    fun isGreetingOrHelpRequest(userMessage: String): Boolean {
        val normalized = userMessage.trim().lowercase()
        if (normalized.isEmpty()) return false
        if (appearsBankingRelated(normalized)) return false
        if (greetingPhrases.any { normalized == it }) return true
        if (normalized.length <= 40 && greetingPhrases.any { phrase -> phrase in normalized }) return true
        return false
    }

    fun shouldShowGreeting(classification: IntentClassification, userMessage: String): Boolean {
        if (!isGreetingOrHelpRequest(userMessage)) return false
        return when (classification.intent) {
            IntentLabel.CHECK_BALANCE,
            IntentLabel.PAY_CREDIT_CARD,
            IntentLabel.TRANSFER_OWN_ACCOUNTS,
            IntentLabel.TRANSFER_THIRD_PARTY,
            IntentLabel.VIEW_CHAT_HISTORY,
            IntentLabel.MONTHLY_EXPENSES,
            -> false
            IntentLabel.AMBIGUOUS,
            IntentLabel.OUT_OF_SCOPE,
            -> true
        }
    }

    fun shouldAskClarification(classification: IntentClassification): Boolean =
        classification.clarificationNeeded ||
            classification.intent == IntentLabel.AMBIGUOUS ||
            classification.confidence < LOW_CONFIDENCE_THRESHOLD

    fun shouldShowSupport(classification: IntentClassification, userMessage: String): Boolean {
        if (classification.intent != IntentLabel.OUT_OF_SCOPE) return false
        if (isGreetingOrHelpRequest(userMessage)) return false
        if (classification.confidence >= OUT_OF_SCOPE_HIGH_CONFIDENCE_THRESHOLD) return true
        return !appearsBankingRelated(userMessage)
    }

    fun shouldRecheckOutOfScope(classification: IntentClassification, userMessage: String): Boolean =
        classification.intent == IntentLabel.OUT_OF_SCOPE &&
            classification.confidence < OUT_OF_SCOPE_HIGH_CONFIDENCE_THRESHOLD &&
            appearsBankingRelated(userMessage)

    fun appearsBankingRelated(text: String): Boolean {
        val normalized = text.lowercase()
        val keywords = listOf(
            "saldo", "pagar", "pago", "tarjeta", "transfer", "cuenta", "movimientos", "deuda",
            "gasto", "gastos", "categoria", "categoría", "historial", "conversacion", "conversación",
            "chat", "donde va mi dinero", "dónde va mi dinero",
        )
        return keywords.any { it in normalized }
    }
}
