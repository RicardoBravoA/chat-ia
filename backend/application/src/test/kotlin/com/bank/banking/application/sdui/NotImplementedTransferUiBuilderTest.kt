package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.IntentLabel
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.sdui.UiComponentType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking

class NotImplementedTransferUiBuilderTest {

    @Test
    fun `TRANSFER_OWN_ACCOUNTS shows banner for own transfers`() = runBlocking {
        val ctx = ctxFor(IntentLabel.TRANSFER_OWN_ACCOUNTS)
        val tree = NotImplementedTransferUiBuilder().build(ctx)

        assertEquals(UiComponentType.COLUMN, tree.type)

        val info = tree.children.first { it.type == UiComponentType.INFO_BANNER }
        assertTrue(info.props["text"].orEmpty().contains("transferencias entre tus cuentas"))
    }

    @Test
    fun `TRANSFER_THIRD_PARTY shows banner for third party transfers`() = runBlocking {
        val ctx = ctxFor(IntentLabel.TRANSFER_THIRD_PARTY)
        val tree = NotImplementedTransferUiBuilder().build(ctx)

        assertEquals(UiComponentType.COLUMN, tree.type)

        val info = tree.children.first { it.type == UiComponentType.INFO_BANNER }
        assertTrue(info.props["text"].orEmpty().contains("transferencias a terceros"))
    }

    private fun ctxFor(intent: IntentLabel): ChatUiBuildContext {
        val token = SessionToken("tok")
        val classification = IntentClassification(
            intent = intent,
            confidence = 0.9,
            entities = emptyMap(),
            clarificationNeeded = false,
            reason = "test",
            source = "test",
        )

        return ChatUiBuildContext(
            token = token,
            userMessage = "transfer",
            classification = classification,
        )
    }
}

