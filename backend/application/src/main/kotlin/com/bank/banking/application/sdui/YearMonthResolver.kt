package com.bank.banking.application.sdui

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

object YearMonthResolver {
    private val yearMonthPattern = Regex("""\d{4}-\d{2}""")
    private val yearMonthInText = Regex("""20\d{2}-\d{2}""")
    private val yearInText = Regex("""20\d{2}""")

    private val numericMonthYearPatterns = listOf(
        Regex("""\b(0?[1-9]|1[0-2])/(20\d{2})\b""") to { month: Int, year: Int -> YearMonth.of(year, month) },
        Regex("""\b(0?[1-9]|1[0-2])-(20\d{2})\b""") to { month: Int, year: Int -> YearMonth.of(year, month) },
        Regex("""\b(20\d{2})/(0?[1-9]|1[0-2])\b""") to { month: Int, year: Int -> YearMonth.of(year, month) },
    )

    private val monthTypoReplacements = mapOf(
        "enenro" to "enero",
        "eneero" to "enero",
        "febreo" to "febrero",
        "setiembre" to "septiembre",
    )

    private val spanishMonths: List<Pair<Int, List<String>>> = listOf(
        1 to listOf("enero"),
        2 to listOf("febrero"),
        3 to listOf("marzo"),
        4 to listOf("abril"),
        5 to listOf("mayo"),
        6 to listOf("junio"),
        7 to listOf("julio"),
        8 to listOf("agosto"),
        9 to listOf("septiembre", "setiembre"),
        10 to listOf("octubre"),
        11 to listOf("noviembre"),
        12 to listOf("diciembre"),
    )

    fun resolve(
        entities: Map<String, String>,
        userMessage: String = "",
        fallback: String = currentYearMonth(),
    ): String {
        val parsedFromMessage = parseFromUserMessage(userMessage)
        if (parsedFromMessage != null) return parsedFromMessage

        entities["yearMonth"]?.trim()?.takeIf { yearMonthPattern.matches(it) }?.let { return it }
        entities["month"]?.trim()?.let { normalizeEntityMonth(it) }?.let { return it }
        return fallback
    }

    fun parseFromUserMessage(message: String): String? {
        val normalized = normalizeMonthTypos(normalize(message))
        if (normalized.isEmpty()) return null

        yearMonthInText.find(normalized)?.value?.let { return it }

        parseNumericMonthYear(normalized)?.let { return it.toString() }

        if ("mes pasado" in normalized || "del mes pasado" in normalized) {
            return YearMonth.now().minusMonths(1).toString()
        }

        val explicitYear = yearInText.find(normalized)?.value?.toIntOrNull()

        for ((monthNumber, names) in spanishMonths) {
            for (name in names) {
                monthWithYearPatterns(name).forEach { pattern ->
                    pattern.find(normalized)?.let { match ->
                        return YearMonth.of(match.groupValues[1].toInt(), monthNumber).toString()
                    }
                }

                if (containsMonthName(normalized, name)) {
                    val year = explicitYear ?: YearMonth.now().year
                    return YearMonth.of(year, monthNumber).toString()
                }
            }
        }

        return null
    }

    /**
     * Formatos soportados junto al nombre del mes:
     * - enero del 2025 / enero de 2025
     * - de enero del 2025
     * - 2025 enero / enero 2025
     */
    private fun monthWithYearPatterns(monthName: String): List<Regex> = listOf(
        Regex("""\b$monthName\b\s+(20\d{2})\b"""),
        Regex("""\b$monthName\b\s+(?:de(?:l)?\s+)(20\d{2})\b"""),
        Regex("""\bde\s+$monthName\b\s+(?:de(?:l)?\s+)?(20\d{2})\b"""),
        Regex("""\b(20\d{2})\b\s+(?:de(?:l)?\s+)?$monthName\b"""),
    )

    private fun parseNumericMonthYear(text: String): YearMonth? {
        for ((pattern, builder) in numericMonthYearPatterns) {
            val match = pattern.find(text) ?: continue
            val groups = match.groupValues
            return if (pattern.pattern.startsWith("""\b(20""")) {
                builder(groups[2].toInt(), groups[1].toInt())
            } else {
                builder(groups[1].toInt(), groups[2].toInt())
            }
        }
        return null
    }

    private fun normalizeEntityMonth(raw: String): String? {
        if (yearMonthPattern.matches(raw)) return raw
        parseNumericMonthYear(normalizeMonthTypos(normalize(raw)))?.let { return it.toString() }
        return null
    }

    private fun containsMonthName(text: String, monthName: String): Boolean =
        Regex("""\b$monthName\b""").containsMatchIn(text)

    private fun normalizeMonthTypos(text: String): String {
        var result = text
        monthTypoReplacements.forEach { (typo, correct) ->
            result = result.replace(Regex("""\b$typo\b"""), correct)
        }
        return result
    }

    private fun normalize(message: String): String =
        message.trim()
            .lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ñ', 'n')

    fun currentYearMonth(): String = YearMonth.now().toString()

    fun formatForDisplay(yearMonth: String): String {
        val parsed = YearMonth.parse(yearMonth)
        val locale = Locale.forLanguageTag("es-ES")
        val monthName = parsed.month.getDisplayName(TextStyle.FULL, locale)
        return "${monthName.replaceFirstChar { it.titlecase(locale) }} ${parsed.year}"
    }
}
