package com.bank.banking.domain.port

import com.bank.banking.domain.model.ChatHistoryMessage
import com.bank.banking.domain.model.IntentClassification

interface IntentClassifierPort {
    suspend fun classify(
        message: String,
        history: List<ChatHistoryMessage> = emptyList(),
    ): IntentClassification
}
