package com.bank.banking.domain.port

import com.bank.banking.domain.model.SessionToken

interface SecureTokenGenerator {
    fun newSessionToken(): SessionToken
    fun newMovementId(): String
}
