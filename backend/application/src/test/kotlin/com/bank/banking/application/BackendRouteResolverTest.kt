package com.bank.banking.application

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackendRouteResolverTest {

    @Test
    fun `CHECK_BALANCE maps to GET v1 me balance endpoint`() {
        val hints = BackendRouteResolver.resolve(classification(IntentLabel.CHECK_BALANCE))

        assertEquals(1, hints.size)
        val h = hints.single()
        assertEquals("GET", h.method)
        assertEquals("/v1/me/balance", h.pathTemplate)
        assertTrue(h.requiresAuth)
        assertFalse(h.requiresIdempotencyKey)
        assertTrue(h.implemented)
    }

    @Test
    fun `PAY_CREDIT_CARD maps to list cards and pay endpoint`() {
        val hints = BackendRouteResolver.resolve(classification(IntentLabel.PAY_CREDIT_CARD))

        assertEquals(2, hints.size)

        val list = hints[0]
        assertEquals("GET", list.method)
        assertEquals("/v1/credit-cards/with-debt", list.pathTemplate)
        assertTrue(list.requiresAuth)
        assertFalse(list.requiresIdempotencyKey)
        assertTrue(list.implemented)

        val pay = hints[1]
        assertEquals("POST", pay.method)
        assertEquals("/v1/credit-cards/{cardId}/payments", pay.pathTemplate)
        assertTrue(pay.requiresAuth)
        assertTrue(pay.requiresIdempotencyKey)
        assertTrue(pay.implemented)
    }

    @Test
    fun `TRANSFER_OWN_ACCOUNTS maps to not implemented endpoint`() {
        val hints = BackendRouteResolver.resolve(classification(IntentLabel.TRANSFER_OWN_ACCOUNTS))

        assertEquals(1, hints.size)
        val h = hints.single()
        assertEquals("POST", h.method)
        assertEquals("/v1/transfers/own", h.pathTemplate)
        assertTrue(h.requiresAuth)
        assertTrue(h.requiresIdempotencyKey)
        assertFalse(h.implemented)
    }

    @Test
    fun `TRANSFER_THIRD_PARTY maps to not implemented endpoint`() {
        val hints = BackendRouteResolver.resolve(classification(IntentLabel.TRANSFER_THIRD_PARTY))

        assertEquals(1, hints.size)
        val h = hints.single()
        assertEquals("POST", h.method)
        assertEquals("/v1/transfers/third-party", h.pathTemplate)
        assertTrue(h.requiresAuth)
        assertTrue(h.requiresIdempotencyKey)
        assertFalse(h.implemented)
    }

    @Test
    fun `AMBIGUOUS maps to empty hints`() {
        val hints = BackendRouteResolver.resolve(classification(IntentLabel.AMBIGUOUS))
        assertTrue(hints.isEmpty())
    }

    @Test
    fun `OUT_OF_SCOPE maps to empty hints`() {
        val hints = BackendRouteResolver.resolve(classification(IntentLabel.OUT_OF_SCOPE))
        assertTrue(hints.isEmpty())
    }

    private fun classification(intent: IntentLabel) =
        IntentClassification(
            intent = intent,
            confidence = 0.9,
            entities = emptyMap(),
            clarificationNeeded = false,
            reason = "test",
            source = "test",
        )
}

