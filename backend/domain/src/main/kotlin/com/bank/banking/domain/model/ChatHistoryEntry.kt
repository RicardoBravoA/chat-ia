package com.bank.banking.domain.model

data class ChatHistoryEntry(
    val sessionId: String,
    val turnCount: Int,
    val lastUserMessage: String,
    val lastIntent: String?,
    val updatedAtEpochMs: Long,
)
