package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@Composable
fun HomeMovementsSectionHeader(
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Historial de movimientos",
            color = AppPalette.LoginTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onSeeAllClick) {
            Text(
                text = "Ver todo",
                color = AppPalette.PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}
