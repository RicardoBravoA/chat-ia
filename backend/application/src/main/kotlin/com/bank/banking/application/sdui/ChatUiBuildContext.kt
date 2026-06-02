package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentClassification
import com.bank.banking.domain.model.SessionToken

data class ChatUiBuildContext(
    val token: SessionToken,
    val userMessage: String,
    val classification: IntentClassification,
)
