package com.bank.mobile.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.organisms.LoginFormCard

@Composable
fun LoginScreen(
    email: String,
    password: String,
    error: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPalette.LoginScreenBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoginFormCard(
            email = email,
            password = password,
            error = error,
            isLoading = isLoading,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onLoginClick = onLoginClick,
        )
    }
}
