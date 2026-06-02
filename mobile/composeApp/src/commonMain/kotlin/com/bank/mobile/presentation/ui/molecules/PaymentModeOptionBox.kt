package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@Composable
fun PaymentModeOptionBox(
    label: String,
    amount: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val borderColor = if (selected) AppPalette.PrimaryBlue else Color(0xFFE2E8F0)
    val bg = if (selected) AppPalette.PrimaryBlue.copy(alpha = 0.06f) else Color(0xFFFAFAFA)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.ChatTextMuted,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AppPalette.ChatTextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}
