package com.bank.mobile.platform

import com.bank.mobile.domain.model.BiometricAuthRequest
import com.bank.mobile.domain.model.BiometricAuthResult
import com.bank.mobile.domain.model.BiometricAvailability
import com.bank.mobile.domain.repository.BiometricAuthenticator
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

@OptIn(ExperimentalForeignApi::class)
actual fun createBiometricAuthenticator(): BiometricAuthenticator = IosBiometricAuthenticator()

@OptIn(ExperimentalForeignApi::class)
private class IosBiometricAuthenticator : BiometricAuthenticator {

    override fun availability(): BiometricAvailability {
        val context = LAContext()
        val canUseBiometrics = context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null,
        )
        if (canUseBiometrics) {
            return BiometricAvailability.BiometricOrDeviceCredential
        }
        val canUseDeviceAuth = context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            error = null,
        )
        return if (canUseDeviceAuth) {
            BiometricAvailability.DeviceCredentialOnly
        } else {
            BiometricAvailability.Unavailable
        }
    }

    override suspend fun authenticate(request: BiometricAuthRequest): BiometricAuthResult {
        if (availability() == BiometricAvailability.Unavailable) {
            return BiometricAuthResult.Unavailable(
                "Configura Face ID, Touch ID o código del dispositivo",
            )
        }
        // DeviceOwnerAuthentication permite biométrico y fallback a código del dispositivo.
        val policy = LAPolicyDeviceOwnerAuthentication

        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val context = LAContext()
                context.evaluatePolicy(
                    policy = policy,
                    localizedReason = request.subtitle,
                ) { success, error ->
                    if (!continuation.isActive) return@evaluatePolicy
                    when {
                        success -> continuation.resume(BiometricAuthResult.Success)
                        error == null -> continuation.resume(BiometricAuthResult.Failed("Autenticación fallida"))
                        error.code == LAErrorUserCancel || error.code == LAErrorSystemCancel ->
                            continuation.resume(BiometricAuthResult.Cancelled)
                        error.code == LAErrorAuthenticationFailed ->
                            continuation.resume(BiometricAuthResult.Failed("Autenticación fallida"))
                        else -> continuation.resume(BiometricAuthResult.Failed(error.localizedDescription))
                    }
                }
            }
        }
    }
}
