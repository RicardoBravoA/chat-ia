package com.bank.mobile.domain.usecase

import com.bank.mobile.domain.model.BiometricAuthRequest
import com.bank.mobile.domain.model.BiometricAuthResult
import com.bank.mobile.domain.model.BiometricAvailability
import com.bank.mobile.domain.repository.BiometricAuthenticator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfirmSensitiveActionUseCaseTest {

    @Test
    fun delegatesToBiometricAuthenticatorWithPaymentCopy() = runTest {
        val fake = FakeBiometricAuthenticator()
        val useCase = ConfirmSensitiveActionUseCase(fake)

        val result = useCase.confirmCreditCardPayment("Oro")

        assertEquals(BiometricAuthResult.Success, result)
        assertEquals("Confirmar pago", fake.lastRequest?.title)
        assertTrue(fake.lastRequest?.subtitle?.contains("Oro") == true)
    }

    private class FakeBiometricAuthenticator(
        private val result: BiometricAuthResult = BiometricAuthResult.Success,
    ) : BiometricAuthenticator {
        var lastRequest: BiometricAuthRequest? = null

        override fun availability(): BiometricAvailability = BiometricAvailability.BiometricOrDeviceCredential

        override suspend fun authenticate(request: BiometricAuthRequest): BiometricAuthResult {
            lastRequest = request
            return result
        }
    }
}
