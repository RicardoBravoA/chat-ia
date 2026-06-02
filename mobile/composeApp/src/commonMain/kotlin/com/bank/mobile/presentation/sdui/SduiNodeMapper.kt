package com.bank.mobile.presentation.sdui

import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardChatAction
import com.bank.mobile.domain.model.sdui.UiComponentType
import com.bank.mobile.domain.model.sdui.UiNode

fun UiNode.toPayCardChatAction(): PayCardChatAction? {
    if (type != UiComponentType.PAY_CARD_PANEL) return null
    val cardId = props["cardId"] ?: return null
    return PayCardChatAction(
        cardId = cardId,
        alias = props["alias"] ?: "",
        lastFourDigits = props["lastFourDigits"] ?: "",
        cardholderName = props["cardholderName"] ?: "",
        expiryMonth = props["expiryMonth"]?.toIntOrNull() ?: return null,
        expiryYear = props["expiryYear"]?.toIntOrNull() ?: return null,
        debt = props["debt"]?.toDoubleOrNull() ?: return null,
        creditLine = props["creditLine"]?.toDoubleOrNull() ?: 0.0,
        minimumPaymentDue = props["minimumPaymentDue"]?.toDoubleOrNull() ?: 0.0,
        statementBalanceDue = props["statementBalanceDue"]?.toDoubleOrNull() ?: 0.0,
        currency = props["currency"] ?: "PEN",
    )
}

fun UiNode.propText(key: String): String = props[key].orEmpty()

fun UiNode.flattenForChat(): List<UiNode> =
    when (type) {
        UiComponentType.COLUMN -> children.flatMap { it.flattenForChat() }
        else -> listOf(this)
    }
