package com.bank.banking.application.usecase

import com.bank.banking.domain.error.AuthenticationException
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.model.User
import com.bank.banking.domain.model.UserId
import com.bank.banking.domain.port.PasswordHasher
import com.bank.banking.domain.port.SecureTokenGenerator
import com.bank.banking.domain.port.SessionRepository
import com.bank.banking.domain.port.UserCredentialsRepository
import com.bank.banking.domain.port.UserRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals

class LoginUseCaseTest {
    private val user = User(UserId("u1"), "a@b.com")

    private val users = object : UserRepository {
        override suspend fun findByEmail(email: String) = if (email == "a@b.com") user else null
        override suspend fun findById(id: UserId) = user
    }

    private val creds = object : UserCredentialsRepository {
        override suspend fun findPasswordHashByUserId(userId: UserId) = "hash"
    }

    private val hasher = object : PasswordHasher {
        override fun verify(plainPassword: String, storedHash: String) = plainPassword == "ok"
    }

    private val sessions = object : SessionRepository {
        var saved: SessionToken? = null
        override suspend fun create(userId: UserId, token: SessionToken, expiresAt: Instant) {
            saved = token
        }

        override suspend fun findValid(token: SessionToken) = null
        override suspend fun revoke(token: SessionToken) {}
    }

    private val tokens = object : SecureTokenGenerator {
        override fun newSessionToken() = SessionToken("tok")
        override fun newMovementId() = "m1"
    }

    private val useCase = LoginUseCase(users, creds, hasher, sessions, tokens)

    @Test
    fun `login success returns token`() = runBlocking {
        val t = useCase.execute("a@b.com", "ok")
        assertEquals("tok", t.value)
        assertEquals("tok", sessions.saved?.value)
    }

    @Test
    fun `wrong password fails`() = runBlocking {
        assertThrows<AuthenticationException> {
            useCase.execute("a@b.com", "bad")
        }
    }
}
