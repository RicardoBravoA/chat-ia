package com.bank.banking.api

import com.bank.banking.domain.model.SessionToken
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header

internal fun ApplicationCall.bearerToken(): SessionToken? {
    val h = request.headers["Authorization"] ?: return null
    if (!h.startsWith("Bearer ")) return null
    val raw = h.removePrefix("Bearer ").trim()
    if (raw.isEmpty()) return null
    return SessionToken(raw)
}
