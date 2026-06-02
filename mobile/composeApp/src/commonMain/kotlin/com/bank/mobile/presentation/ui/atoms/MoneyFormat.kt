package com.bank.mobile.presentation.ui.atoms

import kotlin.math.abs
import kotlin.math.round

/**
 * [amount] en la misma escala que el backend (Double). PEN se muestra como `S/ 1,234.56`.
 */
fun formatMoney(amount: Double, currency: String): String {
    val formatted = formatAmountToTwoDecimals(amount)
    return when (currency.uppercase()) {
        "PEN" -> "S/ $formatted"
        else -> "$currency $formatted"
    }
}

private fun formatAmountToTwoDecimals(amount: Double): String {
    val negative = amount < 0
    val v = abs(amount)
    val totalCents = round(v * 100.0).toLong()
    val intPart = totalCents / 100
    val frac = kotlin.math.abs(totalCents % 100).toInt()
    val fracStr = frac.toString().padStart(2, '0')
    val intWithSep = addThousandsSeparator(intPart)
    val core = "$intWithSep.$fracStr"
    return if (negative) "-$core" else core
}

private fun addThousandsSeparator(value: Long): String {
    val s = value.toString()
    if (s.length <= 3) return s
    val reversed = s.reversed()
    val chunks = reversed.chunked(3)
    return chunks.joinToString(",").reversed()
}
