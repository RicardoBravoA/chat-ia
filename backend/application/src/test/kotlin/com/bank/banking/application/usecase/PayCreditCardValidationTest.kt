package com.bank.banking.application.usecase

import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.error.InvalidPaymentAmountException
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.CreditCardPaymentMode
import com.bank.banking.domain.model.Money
import com.bank.banking.domain.model.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class PayCreditCardValidationTest {

    private val userId = UserId("u1")

    @Test
    fun `MINIMUM resolves to min(minimumPaymentDue, debt)`() {
        val card = card(debt = Money(30.0, "PEN"), minimumPaymentDue = Money(50.0, "PEN"), statementBalanceDue = Money(99.0, "PEN"))
        val cmd = PayCreditCardCommand(
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            mode = CreditCardPaymentMode.MINIMUM,
        )

        val amount = resolvePaymentAmount(card, cmd)
        assertEquals(Money(30.0, "PEN"), amount)
    }

    @Test
    fun `MINIMUM throws when minimumPaymentDue is zero`() {
        val card = card(debt = Money(30.0, "PEN"), minimumPaymentDue = Money(0.0, "PEN"), statementBalanceDue = Money(99.0, "PEN"))
        val cmd = PayCreditCardCommand(
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            mode = CreditCardPaymentMode.MINIMUM,
        )

        assertThrows<InvalidPaymentAmountException> {
            resolvePaymentAmount(card, cmd)
        }
    }

    @Test
    fun `STATEMENT_MONTH resolves to min(statementBalanceDue, debt)`() {
        val card = card(debt = Money(30.0, "PEN"), minimumPaymentDue = Money(10.0, "PEN"), statementBalanceDue = Money(200.0, "PEN"))
        val cmd = PayCreditCardCommand(
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            mode = CreditCardPaymentMode.STATEMENT_MONTH,
        )

        val amount = resolvePaymentAmount(card, cmd)
        assertEquals(Money(30.0, "PEN"), amount)
    }

    @Test
    fun `FULL_DEBT throws when card has no debt`() {
        val card = card(debt = Money(0.0, "PEN"), minimumPaymentDue = Money(10.0, "PEN"), statementBalanceDue = Money(0.0, "PEN"))
        val cmd = PayCreditCardCommand(
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            mode = CreditCardPaymentMode.FULL_DEBT,
        )

        assertThrows<InvalidPaymentAmountException> {
            resolvePaymentAmount(card, cmd)
        }
    }

    @Test
    fun `CUSTOM throws when customAmount is missing`() {
        val card = card(debt = Money(100.0, "PEN"))
        val cmd = PayCreditCardCommand(
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            mode = CreditCardPaymentMode.CUSTOM,
            customAmount = null,
        )

        assertThrows<InvalidPaymentAmountException> {
            resolvePaymentAmount(card, cmd)
        }
    }

    @Test
    fun `CUSTOM throws when customAmount is greater than debt`() {
        val card = card(debt = Money(100.0, "PEN"))
        val cmd = PayCreditCardCommand(
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            mode = CreditCardPaymentMode.CUSTOM,
            customAmount = 150.0,
        )

        assertThrows<InvalidPaymentAmountException> {
            resolvePaymentAmount(card, cmd)
        }
    }

    @Test
    fun `CUSTOM resolves within bounds`() {
        val card = card(debt = Money(100.0, "PEN"))
        val cmd = PayCreditCardCommand(
            cardholderName = "MARIA LOPEZ",
            expiryMonth = 12,
            expiryYear = 2028,
            mode = CreditCardPaymentMode.CUSTOM,
            customAmount = 40.0,
        )

        val amount = resolvePaymentAmount(card, cmd)
        assertEquals(Money(40.0, "PEN"), amount)
    }

    @Test
    fun `cardholdersMatch normalizes whitespace and matches case-insensitively`() {
        assertTrue(cardholdersMatch("Maria   Lopez", "  maria  lopez  "))
        assertFalse(cardholdersMatch("Maria Lopez", "Ana Lopez"))
    }

    @Test
    fun `verifyExpiryMatchesCard throws on expiry mismatch`() {
        val card = card(expiryMonth = 12, expiryYear = 2028)

        assertThrows<BadRequestException> {
            verifyExpiryMatchesCard(card, expiryMonth = 11, expiryYear = 2028)
        }
        assertThrows<BadRequestException> {
            verifyExpiryMatchesCard(card, expiryMonth = 12, expiryYear = 2027)
        }
    }

    @Test
    fun `verifyExpiryMatchesCard does not throw when expiry matches`() {
        val card = card(expiryMonth = 12, expiryYear = 2028)

        verifyExpiryMatchesCard(card, expiryMonth = 12, expiryYear = 2028)
    }

    private fun card(
        id: CreditCardId = CreditCardId("card-1"),
        userId: UserId = this.userId,
        alias: String = "Oro",
        lastFourDigits: String = "1234",
        cardholderName: String = "MARIA LOPEZ",
        expiryMonth: Int = 12,
        expiryYear: Int = 2028,
        debt: Money = Money(4200.0, "PEN"),
        minimumPaymentDue: Money = Money(420.0, "PEN"),
        statementBalanceDue: Money = Money(1200.0, "PEN"),
    ): CreditCard =
        CreditCard(
            id = id,
            userId = userId,
            alias = alias,
            lastFourDigits = lastFourDigits,
            cardholderName = cardholderName,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            debt = debt,
            creditLine = Money(10_000.0, "PEN"),
            minimumPaymentDue = minimumPaymentDue,
            statementBalanceDue = statementBalanceDue,
        )
}

