package com.bank.banking.domain.port

import com.bank.banking.domain.model.UserId

interface PasswordHasher {
    fun verify(plainPassword: String, storedHash: String): Boolean
}

interface UserCredentialsRepository {
    suspend fun findPasswordHashByUserId(userId: UserId): String?
}
