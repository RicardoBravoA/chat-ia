package com.bank.mobile.domain.repository

import com.bank.mobile.domain.model.BiometricAuthRequest
import com.bank.mobile.domain.model.BiometricAuthResult
import com.bank.mobile.domain.model.BiometricAvailability

interface BiometricAuthenticator {
    fun availability(): BiometricAvailability

    suspend fun authenticate(request: BiometricAuthRequest): BiometricAuthResult
}
