package com.bank.banking.application.usecase

import com.bank.banking.domain.error.UnauthorizedException
import com.bank.banking.domain.model.CreditCard
import com.bank.banking.domain.model.SessionToken
import com.bank.banking.domain.port.CreditCardRepository
import com.bank.banking.domain.port.SessionRepository

class ListCreditCardsWithDebtUseCase(
    private val sessions: SessionRepository,
    private val cards: CreditCardRepository,
) {
    suspend fun execute(token: SessionToken): List<CreditCard> {
        val session = sessions.findValid(token) ?: throw UnauthorizedException()
        return cards.listWithDebtByUser(session.userId)
    }
}
