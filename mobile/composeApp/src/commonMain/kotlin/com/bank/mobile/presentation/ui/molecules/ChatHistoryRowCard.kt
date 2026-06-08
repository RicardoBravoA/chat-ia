package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@Composable
fun ChatHistoryRowCard(
    sessionLabel: String,
    lastMessage: String,
    turnCount: Int,
    timeLabel: String,
    lastIntent: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = sessionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.PrimaryBlue,
                )
                if (timeLabel.isNotBlank()) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppPalette.ChatTextMuted,
                    )
                }
            }
            Text(
                text = lastMessage.ifBlank { "Sin mensajes" },
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.ChatTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$turnCount mensajes · última intención: $lastIntent",
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.ChatTextMuted,
            )
        }
    }
}
