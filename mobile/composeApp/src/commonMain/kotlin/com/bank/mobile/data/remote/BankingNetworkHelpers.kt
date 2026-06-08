package com.bank.mobile.data.remote

import kotlin.random.Random

internal fun newBankingIdempotencyKey(): String {
    val bytes = ByteArray(16)
    Random.Default.nextBytes(bytes)
    return bytes.joinToString("") { b ->
        (b.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
}
