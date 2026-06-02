package com.bank.banking.domain.port

import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import kotlinx.datetime.Instant

data class SessionRecord(
    val token: SessionToken,
    val userId: UserId,
    val expiresAt: Instant,
)

interface SessionRepository {
    suspend fun create(userId: UserId, token: SessionToken, expiresAt: Instant)
    suspend fun findValid(token: SessionToken): SessionRecord?
    suspend fun revoke(token: SessionToken)
}
