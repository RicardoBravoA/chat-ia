package com.bank.mobile.domain.model

enum class BiometricAvailability {
    /** Huella, rostro u otro biométrico; con fallback a PIN/patrón del dispositivo. */
    BiometricOrDeviceCredential,

    /** Sin biométrico enrolado; solo PIN/patrón/contraseña del dispositivo. */
    DeviceCredentialOnly,

    /** Sin bloqueo de pantalla ni biométrico usable. */
    Unavailable,
}

data class BiometricAuthRequest(
    val title: String,
    val subtitle: String,
)

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult

    data object Cancelled : BiometricAuthResult

    data class Unavailable(val reason: String) : BiometricAuthResult

    data class Failed(val message: String) : BiometricAuthResult
}
