package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@Composable
fun ChatAssistantHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppPalette.ChatHeaderBar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(AppPalette.PrimaryBlue, AppPalette.CardNavyBottom),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "A",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Asistente IA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.ChatTextPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppPalette.OnlineDot),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "EN LÍNEA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = AppPalette.ChatTextMuted,
                    letterSpacing = 0.5.sp,
                )
            }
        }
        Text(
            text = "✕",
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClose)
                .padding(12.dp),
            color = AppPalette.ChatTextMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
