package com.bank.mobile.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS no expone botón "atrás" global; la navegación atrás va por la cabecera del chat.
}
