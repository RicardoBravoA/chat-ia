package com.bank.banking.infra.mongo

import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.SecureTokenGenerator
import java.security.SecureRandom
import java.util.HexFormat

class SecureTokenGeneratorImpl : SecureTokenGenerator {
    private val random = SecureRandom()

    override fun newSessionToken(): SessionToken {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return SessionToken(HexFormat.of().formatHex(bytes))
    }

    override fun newMovementId(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return "mov_" + HexFormat.of().formatHex(bytes)
    }
}
