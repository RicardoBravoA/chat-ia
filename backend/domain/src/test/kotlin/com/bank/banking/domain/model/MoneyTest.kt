package com.bank.banking.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MoneyTest {
    @Test
    fun `rejects negative minor units`() {
        assertThrows<IllegalArgumentException> {
            Money(-1.0, "PEN")
        }
    }

    @Test
    fun `minus same currency`() {
        val a = Money(100.0, "PEN")
        val b = Money(30.0, "PEN")
        assertEquals(70.0, (a - b).amount, 1e-9)
    }
}
