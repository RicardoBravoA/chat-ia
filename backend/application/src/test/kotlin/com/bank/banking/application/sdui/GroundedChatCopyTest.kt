package com.bank.banking.application.sdui

import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.UserId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroundedChatCopyTest {
    @Test
    fun `greeting intro uses nickname when available`() {
        val text = GroundedChatCopy.greetingIntro("María")
        assertTrue(text.startsWith("Hola, María."))
    }

    @Test
    fun `greeting intro falls back without nickname`() {
        val text = GroundedChatCopy.greetingIntro("")
        assertTrue(text.startsWith("Hola."))
    }

    @Test
    fun `balance intro uses nickname and formatted amount`() {
        val text = GroundedChatCopy.balanceIntro("Nómina", "25000.0 PEN")
        assertTrue(text.contains("Nómina"))
        assertTrue(text.contains("25000.0 PEN"))
    }

    @Test
    fun `pay intro focuses card alias and amount from entities`() {
        val cards = listOf(
            CreditCard(
                id = CreditCardId("card-1"),
                userId = UserId("u1"),
                alias = "Oro",
                lastFourDigits = "1234",
                cardholderName = "MARIA LOPEZ",
                expiryMonth = 12,
                expiryYear = 2028,
                debt = Money(4200.0, "PEN"),
                creditLine = Money(10_000.0, "PEN"),
                minimumPaymentDue = Money(420.0, "PEN"),
                statementBalanceDue = Money(1200.0, "PEN"),
            ),
        )

        val text = GroundedChatCopy.payCardsIntro(
            cards = cards,
            entities = mapOf("cardAlias" to "oro", "amount" to "50"),
        )

        assertTrue(text.contains("Oro"))
        assertTrue(text.contains("4200.0 PEN"))
        assertTrue(text.contains("50 PEN"))
    }

    @Test
    fun `pay intro handles multiple cards`() {
        val cards = listOf(
            sampleCard("Oro"),
            sampleCard("Platino"),
        )

        val text = GroundedChatCopy.payCardsIntro(cards, emptyMap())

        assertEquals("Tienes 2 tarjetas con deuda. Elige la tarjeta y el modo de pago:", text)
    }

    private fun sampleCard(alias: String): CreditCard =
        CreditCard(
            id = CreditCardId("card-$alias"),
            userId = UserId("u1"),
            alias = alias,
            lastFourDigits = "1234",
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            debt = Money(4200.0, "PEN"),
            creditLine = Money(10_000.0, "PEN"),
            minimumPaymentDue = Money(420.0, "PEN"),
            statementBalanceDue = Money(1200.0, "PEN"),
        )
}
