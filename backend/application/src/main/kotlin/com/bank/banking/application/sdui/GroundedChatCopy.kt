package com.bank.banking.application.sdui

import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.Money

object GroundedChatCopy {
    fun greetingIntro(nickname: String): String {
        val greeting = displayName(nickname)?.let { "Hola, $it." } ?: "Hola."
        return "$greeting Soy tu asistente bancario. Puedo ayudarte con tu saldo, pagos de tarjeta, " +
            "gastos del mes e historial de conversaciones. Elige una opción o cuéntame en tus palabras."
    }

    fun balanceIntro(nickname: String, formattedBalance: String): String {
        val accountLabel = nickname.trim().ifBlank { "tu cuenta principal" }
        return "Claro. Revisé $accountLabel y este es tu saldo disponible: $formattedBalance."
    }

    fun balanceFollowUp(): String =
        "Si quieres, también puedo ayudarte a pagar tu tarjeta o revisar otra operación."

    fun payCardsEmpty(): String =
        "Revisé tus tarjetas y no encontré deuda pendiente en este momento."

    fun payCardsIntro(cards: List<CreditCard>, entities: Map<String, String>): String {
        if (cards.isEmpty()) return payCardsEmpty()

        val amountSuffix = entities["amount"]?.trim()?.takeIf { it.isNotEmpty() }?.let { amount ->
            " por un monto de $amount ${cards.first().debt.currency}"
        }.orEmpty()

        val cardAlias = entities.cardAlias()
        if (cardAlias != null) {
            val card = cards.find { it.alias.equals(cardAlias, ignoreCase = true) }
            if (card != null) {
                return "Perfecto. Tu tarjeta ${card.alias} tiene una deuda de ${card.debt.formatForDisplay()}" +
                    "$amountSuffix. Elige el modo de pago:"
            }
        }

        if (cards.size == 1) {
            val card = cards.first()
            return "Encontré una tarjeta con deuda: ${card.alias} (${card.debt.formatForDisplay()})" +
                "$amountSuffix. Indica el modo de pago:"
        }

        return "Tienes ${cards.size} tarjetas con deuda$amountSuffix. Elige la tarjeta y el modo de pago:"
    }

    fun transferUnavailable(operationLabel: String): String =
        "Entendido. Por ahora $operationLabel aún no está disponible en el chat, " +
            "pero puedo ayudarte con saldo o pago de tarjeta."

    fun chatHistoryEmpty(): String =
        "Aún no tienes conversaciones guardadas con el asistente. Envía un mensaje y aparecerán aquí."

    fun chatHistoryIntro(sessionCount: Int): String =
        "Estas son tus últimas $sessionCount conversaciones con el asistente:"

    fun chatHistoryFollowUp(): String =
        "Puedes retomar cualquier tema escribiendo de nuevo, por ejemplo tu saldo o gastos del mes."

    fun monthlyExpensesEmpty(monthLabel: String): String =
        "No encontré gastos categorizados en $monthLabel, ."

    fun monthlyExpensesIntro(
        monthLabel: String,
        totalTransactions: Int,
        grandTotal: Double,
        currency: String,
    ): String {
        val rounded = kotlin.math.round(grandTotal * 100.0) / 100.0
        return "Resumen de gastos en $monthLabel: $totalTransactions transacciones por un total de " +
            "$rounded $currency, agrupadas por categoría:"
    }

    fun monthlyExpensesFollowUp(): String =
        "Si quieres otro mes, indícamelo (por ejemplo: gastos de 2024-06)."

    private fun Map<String, String>.cardAlias(): String? =
        this["cardAlias"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: this["card_alias"]?.trim()?.takeIf { it.isNotEmpty() }

    private fun displayName(nickname: String): String? =
        nickname.trim().takeIf { it.isNotEmpty() }
}
