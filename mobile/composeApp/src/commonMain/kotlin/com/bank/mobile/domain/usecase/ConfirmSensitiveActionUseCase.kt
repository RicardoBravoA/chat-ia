package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.model.BiometricAuthRequest
import com.bank.mobile.domain.model.BiometricAuthResult
import com.bank.mobile.domain.repository.BiometricAuthenticator

class ConfirmSensitiveActionUseCase(
    private val biometricAuthenticator: BiometricAuthenticator,
) {
    suspend fun confirmCreditCardPayment(cardAlias: String): BiometricAuthResult {
        val alias = cardAlias.trim().ifBlank { "tarjeta" }
        return biometricAuthenticator.authenticate(
            BiometricAuthRequest(
                title = "Confirmar pago",
                subtitle = "Autentícate para pagar tu tarjeta $alias",
            ),
        )
    }
}
