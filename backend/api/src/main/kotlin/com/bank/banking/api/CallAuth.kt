package com.bank.banking.api

import com.bank.banking.domain.error.NotFoundException
import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.CreditCardId
import com.bank.banking.domain.model.SessionToken
import io.ktor.server.application.ApplicationCall

internal fun ApplicationCall.requireBearerToken(): SessionToken =
    bearerToken() ?: throw UnauthorizedException()

/**
 * Sesión válida (token presente y no expirado). Útil cuando el caso de uso no vuelve a validar sesión.
 */
internal suspend fun ApplicationCall.requireSession(root: BankingCompositionRoot): SessionToken {
    val t = requireBearerToken()
    root.sessions.findValid(t) ?: throw UnauthorizedException()
    return t
}

internal fun ApplicationCall.cardIdParameter(): CreditCardId {
    val raw = parameters["cardId"] ?: throw NotFoundException("cardId")
    return CreditCardId(raw)
}
