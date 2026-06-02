package com.bank.banking.infra.mongo

import at.favre.lib.crypto.bcrypt.BCrypt
import com.bank.banking.domain.port.PasswordHasher

class BcryptPasswordHasher(private val cost: Int = 12) : PasswordHasher {
    override fun verify(plainPassword: String, storedHash: String): Boolean =
        BCrypt.verifyer().verify(plainPassword.toCharArray(), storedHash).verified

    fun hash(plainPassword: String): String =
        BCrypt.withDefaults().hashToString(cost, plainPassword.toCharArray())
}
