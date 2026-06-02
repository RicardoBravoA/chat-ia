package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.model.Balance
import com.bank.mobile.domain.repository.HomeRepository

class GetBalanceUseCase(private val repo: HomeRepository) {
    suspend operator fun invoke(token: String): Balance = repo.getBalance(token)
}
