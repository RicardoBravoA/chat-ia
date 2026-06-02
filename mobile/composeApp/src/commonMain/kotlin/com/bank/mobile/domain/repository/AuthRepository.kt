package com.bank.mobile.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): String
}
