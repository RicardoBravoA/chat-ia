package com.bank.banking.domain.port

import com.bank.banking.domain.model.User
import com.bank.banking.domain.model.UserId

interface UserRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: UserId): User?
}
