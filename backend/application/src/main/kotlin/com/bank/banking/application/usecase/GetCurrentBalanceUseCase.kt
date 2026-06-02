package com.bank.banking.application.usecase

import com.bank.banking.domain.error.NotFoundException
import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.AccountRepository
import com.bank.banking.domain.port.SessionRepository

class GetCurrentBalanceUseCase(
    private val sessions: SessionRepository,
    private val accounts: AccountRepository,
) {
    suspend fun execute(token: SessionToken): BalanceQueryResult {
        val session = sessions.findValid(token) ?: throw UnauthorizedException()
        val account = accounts.findPrimaryByUserId(session.userId)
            ?: throw NotFoundException("Account not found")
        return BalanceQueryResult(balance = account.balance, nickname = account.nickname)
    }
}
