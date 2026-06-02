package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@Composable
fun LoginEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CORREO ELECTRÓNICO",
            color = AppPalette.LoginLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            placeholder = {
                Text(
                    "user@domain.com",
                    color = AppPalette.LoginTextMuted.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = AppPalette.LoginTextMuted,
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppPalette.LoginInputBackground,
                unfocusedContainerColor = AppPalette.LoginInputBackground,
                disabledContainerColor = AppPalette.LoginInputBackground.copy(alpha = 0.85f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedTextColor = AppPalette.LoginTextPrimary,
                unfocusedTextColor = AppPalette.LoginTextPrimary,
                disabledTextColor = AppPalette.LoginTextPrimary.copy(alpha = 0.55f),
                cursorColor = AppPalette.LoginPrimaryOrange,
            ),
        )
    }
}
