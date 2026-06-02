package com.bank.mobile.presentation.navigation

import androidx.compose.runtime.Composable

/** Atrás del sistema (Android: botón atrás; iOS: sin hardware back, el usuario usa la cabecera). */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
