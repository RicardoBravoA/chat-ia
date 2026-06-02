package com.bank.mobile.data.remote

import kotlin.random.Random
import kotlinx.coroutines.delay

internal suspend fun simulateBankingNetworkDelay() {
    delay(Random.nextLong(from = 1_000L, until = 3_001L))
}

internal fun newBankingIdempotencyKey(): String {
    val bytes = ByteArray(16)
    Random.Default.nextBytes(bytes)
    return bytes.joinToString("") { b ->
        (b.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
}
