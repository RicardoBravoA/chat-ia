package com.bank.banking.api.intent

import com.bank.banking.domain.model.IntentLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BankingIntentKeywordResolverTest {
    @Test
    fun `resolves pay credit card from gerund with greeting`() {
        val intent = BankingIntentKeywordResolver.resolveAmbiguous(
            "hola, puedes ayudarme pagando mi tc",
        )
        assertEquals(IntentLabel.PAY_CREDIT_CARD, intent)
    }

    @Test
    fun `resolves pay credit card from tc and pay verb`() {
        val intent = BankingIntentKeywordResolver.resolveAmbiguous("quiero pago mi tc hoy")
        assertEquals(IntentLabel.PAY_CREDIT_CARD, intent)
    }

    @Test
    fun `returns null for plain greeting`() {
        val intent = BankingIntentKeywordResolver.resolveAmbiguous("hola, que tal")
        assertEquals(null, intent)
    }
}
