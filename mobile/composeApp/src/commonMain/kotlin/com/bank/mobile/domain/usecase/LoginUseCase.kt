package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.repository.AuthRepository

class LoginUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): String = repo.login(email, password)
}
