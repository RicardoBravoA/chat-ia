package com.bank.banking.application.usecase

import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.port.IntentClassifierPort
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RouteChatMessageUseCaseTest {
    @Test
    fun `blank message throws`() {
        val uc = RouteChatMessageUseCase(IntentClassifierPort { error("unused") })
        assertThrows<BadRequestException> {
            runBlocking { uc.execute("   ") }
        }
    }

    @Test
    fun `check balance intent maps to GET balance`() = runBlocking {
        val uc = RouteChatMessageUseCase(
            IntentClassifierPort {
                IntentClassification(
                    intent = IntentLabel.CHECK_BALANCE,
                    confidence = 0.9,
                    entities = emptyMap(),
                    clarificationNeeded = false,
                    reason = "test",
                    source = "test",
                )
            },
        )
        val r = uc.execute("quiero ver saldos")
        assertEquals(1, r.suggestedActions.size)
        assertEquals("GET", r.suggestedActions[0].method)
        assertEquals("/v1/me/balance", r.suggestedActions[0].pathTemplate)
    }
}
