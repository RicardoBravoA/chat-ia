package com.bank.mobile.presentation.ui.atoms

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Hora para burbujas de chat: `01:05 PM` (12 h + AM/PM). */
fun formatChatTime12h(epochMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour24 = local.hour
    val minute = local.minute
    val amPm = if (hour24 < 12) "AM" else "PM"
    val h = hour24 % 12
    val hour12 = if (h == 0) 12 else h
    val hh = hour12.toString().padStart(2, '0')
    val mm = minute.toString().padStart(2, '0')
    return "$hh:$mm $amPm"
}

private val receiptMonthsEs =
    listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

/** Ej.: "24 Oct, 2023" */
fun formatReceiptDate(epochMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val mon = receiptMonthsEs[local.monthNumber - 1]
    return "${local.dayOfMonth} $mon, ${local.year}"
}

/** Ej.: "#SL-9928341" a partir del id de movimiento del backend. */
fun formatReceiptReference(movementId: String): String {
    val alnum = movementId.filter { it.isLetterOrDigit() }
    val suffix = (alnum.takeLast(7).ifEmpty { movementId }.uppercase()).padStart(7, '0').takeLast(7)
    return "#SL-$suffix"
}

/**
 * Fecha/hora legible para la lista de inicio (p. ej. `24 oct 2023 02:45 p. m.`).
 */
fun formatHomeMovementDateTime(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val mon = receiptMonthsEs[dt.monthNumber - 1].lowercase()
    val hour24 = dt.hour
    val amPm = if (hour24 < 12) "a. m." else "p. m."
    val h12 = hour24 % 12
    val hour12 = if (h12 == 0) 12 else h12
    val hh = hour12.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    return "${dt.dayOfMonth} $mon ${dt.year} $hh:$mm $amPm"
}

/** Fecha/hora para filas de historial de movimientos. */
fun formatMovementDateTime(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.date.dayOfMonth.toString().padStart(2, '0')
    val month = dt.date.monthNumber.toString().padStart(2, '0')
    val year = dt.date.year.toString()
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    val second = dt.second.toString().padStart(2, '0')
    return "$day/$month/$year $hour:$minute:$second"
}
