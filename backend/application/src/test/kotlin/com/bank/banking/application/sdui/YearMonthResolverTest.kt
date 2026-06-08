package com.bank.banking.application.sdui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test

class YearMonthResolverTest {
    @Test
    fun `prefers parsed month from user message over entity`() {
        val resolved = YearMonthResolver.resolve(
            entities = mapOf("yearMonth" to "2024-06"),
            userMessage = "gastos de enero del 2025",
            fallback = "2026-01",
        )
        assertEquals("2025-01", resolved)
    }

    @ParameterizedTest
    @CsvSource(
        "gastos de enero del 2025, 2025-01",
        "gastos de enero de 2025, 2025-01",
        "gastos 2025 enero, 2025-01",
        "gastos enero 2025, 2025-01",
        "muéstrame los gastos de enero del 2025, 2025-01",
        "muestrame los gastos de enero del 2025, 2025-01",
        "gastos enenro 2025, 2025-01",
        "gastos de 01/2025, 2025-01",
        "gastos de 1/2025, 2025-01",
        "gastos de 01-2025, 2025-01",
        "gastos de 1-2025, 2025-01",
        "gastos de 2025/01, 2025-01",
        "quiero ver gastos de 2024-06, 2024-06",
        "gastos de junio 2024, 2024-06",
        "resumen de marzo 2025, 2025-03",
    )
    fun `parses supported month year formats from user message`(message: String, expected: String) {
        assertEquals(expected, YearMonthResolver.parseFromUserMessage(message))
    }

    @Test
    fun `returns null when month cannot be inferred`() {
        assertNull(YearMonthResolver.parseFromUserMessage("gastos del mes"))
    }
}
