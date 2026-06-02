package com.bank.mobile.presentation.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.molecules.LoginEmailField
import com.bank.mobile.presentation.ui.molecules.LoginPasswordField
import com.bank.mobile.presentation.ui.molecules.LoginSubmitButton

@Composable
fun LoginFormCard(
    email: String,
    password: String,
    error: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 420.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Bienvenido",
                color = AppPalette.LoginTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ingresa tus credenciales para acceder",
                color = AppPalette.LoginTextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LoginEmailField(
                    value = email,
                    onValueChange = onEmailChange,
                    enabled = !isLoading,
                )
                LoginPasswordField(
                    value = password,
                    onValueChange = onPasswordChange,
                    enabled = !isLoading,
                )
            }
            Spacer(Modifier.height(28.dp))
            LoginSubmitButton(isLoading = isLoading, onClick = onLoginClick)
            error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
