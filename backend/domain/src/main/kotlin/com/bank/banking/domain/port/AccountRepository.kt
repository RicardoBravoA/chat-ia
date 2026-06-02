package com.bank.banking.domain.port

import com.bank.banking.domain.model.Account
import com.bank.banking.domain.model.UserId

interface AccountRepository {
    suspend fun findPrimaryByUserId(userId: UserId): Account?
}
