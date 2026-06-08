package com.bank.mobile.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.bank.mobile.AndroidContextHolder
import com.bank.mobile.domain.model.BiometricAuthRequest
import com.bank.mobile.domain.model.BiometricAuthResult
import com.bank.mobile.domain.model.BiometricAvailability
import com.bank.mobile.domain.repository.BiometricAuthenticator
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

actual fun createBiometricAuthenticator(): BiometricAuthenticator = AndroidBiometricAuthenticator()

private class AndroidBiometricAuthenticator : BiometricAuthenticator {

    override fun availability(): BiometricAvailability {
        val biometricStatus = BiometricManager.from(AndroidContextHolder.applicationContext)
            .canAuthenticate(BIOMETRIC_STRONG_OR_DEVICE_CREDENTIAL)
        return when (biometricStatus) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.BiometricOrDeviceCredential
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> {
                val deviceOnly = BiometricManager.from(AndroidContextHolder.applicationContext)
                    .canAuthenticate(DEVICE_CREDENTIAL)
                if (deviceOnly == BiometricManager.BIOMETRIC_SUCCESS) {
                    BiometricAvailability.DeviceCredentialOnly
                } else {
                    BiometricAvailability.Unavailable
                }
            }
            else -> BiometricAvailability.Unavailable
        }
    }

    override suspend fun authenticate(request: BiometricAuthRequest): BiometricAuthResult {
        val activity = AndroidContextHolder.currentActivity()
            ?: return BiometricAuthResult.Unavailable("No hay actividad disponible para autenticar")

        val allowedAuthenticators = when (availability()) {
            BiometricAvailability.BiometricOrDeviceCredential -> BIOMETRIC_STRONG_OR_DEVICE_CREDENTIAL
            BiometricAvailability.DeviceCredentialOnly -> DEVICE_CREDENTIAL
            BiometricAvailability.Unavailable ->
                return BiometricAuthResult.Unavailable(
                    "Configura un bloqueo de pantalla o biométrico en el dispositivo",
                )
        }

        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) {
                                continuation.resume(BiometricAuthResult.Success)
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (!continuation.isActive) return
                            val outcome = when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                BiometricPrompt.ERROR_CANCELED,
                                -> BiometricAuthResult.Cancelled
                                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                                BiometricPrompt.ERROR_NO_BIOMETRICS,
                                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                                -> BiometricAuthResult.Unavailable(errString.toString())
                                else -> BiometricAuthResult.Failed(errString.toString())
                            }
                            continuation.resume(outcome)
                        }
                    },
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(request.title)
                    .setSubtitle(request.subtitle)
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .build()

                continuation.invokeOnCancellation {
                    prompt.cancelAuthentication()
                }
                prompt.authenticate(promptInfo)
            }
        }
    }

    private companion object {
        private const val BIOMETRIC_STRONG_OR_DEVICE_CREDENTIAL =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        private const val DEVICE_CREDENTIAL = BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
