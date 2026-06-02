package com.bank.banking.application.usecase

import com.bank.banking.domain.error.AuthenticationException
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.PasswordHasher
import com.bank.banking.domain.port.SecureTokenGenerator
import com.bank.banking.domain.port.SessionRepository
import com.bank.banking.domain.port.UserCredentialsRepository
import com.bank.banking.domain.port.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class LoginUseCase(
    private val users: UserRepository,
    private val credentials: UserCredentialsRepository,
    private val passwordHasher: PasswordHasher,
    private val sessions: SessionRepository,
    private val tokens: SecureTokenGenerator,
    private val clock: Clock = Clock.System,
) {
    private val sessionTtlMs: Long = 24L * 60 * 60 * 1000

    suspend fun execute(email: String, plainPassword: String): SessionToken {
        val normalized = email.trim().lowercase()
        val user = users.findByEmail(normalized) ?: throw AuthenticationException()
        val hash = credentials.findPasswordHashByUserId(user.id) ?: throw AuthenticationException()
        if (!passwordHasher.verify(plainPassword, hash)) throw AuthenticationException()

        val sessionToken = tokens.newSessionToken()
        val expiresAt = Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds() + sessionTtlMs)
        sessions.create(user.id, sessionToken, expiresAt)
        return sessionToken
    }
}
