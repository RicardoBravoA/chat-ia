package com.bank.banking.domain.port

import com.bank.banking.domain.model.IntentClassification

fun interface IntentClassifierPort {
    suspend fun classify(message: String): IntentClassification
}
