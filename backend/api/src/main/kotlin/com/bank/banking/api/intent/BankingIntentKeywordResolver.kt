package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentLabel

/**
 * Resuelve intenciones bancarias evidentes que las reglas literales del JSON pueden omitir
 * (p. ej. "pagando mi tc" vs "pagar mi tc", saludo + operación en la misma frase).
 */
object BankingIntentKeywordResolver {
    private val payVerbPattern =
        Regex("""\b(pagar|pagando|pago|paga|abonar|abonando|abona|liquidar|liquidando|saldar|saldando)\b""")
    private val cardRefPattern =
        Regex("""\b(tc|tarjeta|tarjetas|plastico|plástico|credito|crédito)\b""")
    private val balancePattern =
        Regex("""\b(saldo|saldos|balance|cuanto tengo|cuánto tengo|estado de cuenta)\b""")
    private val transferThirdPattern =
        Regex("""\b(transferir|transferencia|enviar|mandar)\b.*\b(a|para)\b""")
    private val transferOwnPattern =
        Regex("""\b(transferir|transferencia|pasar|mover)\b.*\b(entre mis cuentas|de nomina|de nómina|a ahorro|a mi ahorro)\b""")
    private val chatHistoryPattern =
        Regex("""\b(historial|conversaciones|chats anteriores|mis chats|ver chat)\b""")
    private val monthlyExpensesPattern =
        Regex("""\b(gastos?|donde va mi dinero|dónde va mi dinero|categorias de gasto|categorías de gasto)\b""")

    fun resolveAmbiguous(text: String): IntentLabel? {
        val normalized = text.trim().lowercase()
        if (normalized.isEmpty()) return null
        return when {
            matchesPayCreditCard(normalized) -> IntentLabel.PAY_CREDIT_CARD
            chatHistoryPattern.containsMatchIn(normalized) -> IntentLabel.VIEW_CHAT_HISTORY
            monthlyExpensesPattern.containsMatchIn(normalized) -> IntentLabel.MONTHLY_EXPENSES
            transferOwnPattern.containsMatchIn(normalized) -> IntentLabel.TRANSFER_OWN_ACCOUNTS
            transferThirdPattern.containsMatchIn(normalized) -> IntentLabel.TRANSFER_THIRD_PARTY
            balancePattern.containsMatchIn(normalized) -> IntentLabel.CHECK_BALANCE
            else -> null
        }
    }

    private fun matchesPayCreditCard(normalized: String): Boolean {
        if (payVerbPattern.containsMatchIn(normalized) && cardRefPattern.containsMatchIn(normalized)) {
            return true
        }
        return Regex("""\btc\b""").containsMatchIn(normalized) &&
            listOf("pagar", "pagando", "pago", "abonar", "abonando", "liquidar", "saldar", "deuda")
                .any { it in normalized }
    }
}
