package com.bank.banking.domain.port

import com.bank.banking.domain.model.UserId

interface IdempotencyRepository {
    /**
     * Reserva la clave; devuelve false si ya existía (reintento) — el caller debe devolver el resultado almacenado.
     */
    suspend fun tryBegin(key: String, userId: UserId): Boolean

    suspend fun complete(key: String, resultSummary: String)
    suspend fun getResult(key: String): String?
}
