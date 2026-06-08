package com.bank.banking.application.sdui

import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.model.IntentLabel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChatQuickReplyPolicyTest {
    @Test
    fun `allows greeting quick reply intents only`() {
        assertEquals(IntentLabel.CHECK_BALANCE, ChatQuickReplyPolicy.parseAllowedQuickReplyIntent("CHECK_BALANCE"))
        assertEquals(IntentLabel.PAY_CREDIT_CARD, ChatQuickReplyPolicy.parseAllowedQuickReplyIntent("pay_credit_card"))
        assertNull(ChatQuickReplyPolicy.parseAllowedQuickReplyIntent("AMBIGUOUS"))
    }

    @Test
    fun `rejects unknown quick reply intent`() {
        assertThrows<BadRequestException> {
            ChatQuickReplyPolicy.requireAllowedQuickReplyIntent("OUT_OF_SCOPE")
        }
    }
}
