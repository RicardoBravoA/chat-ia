package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bank.mobile.presentation.ui.atoms.AppPalette

private val introBody =
    "Lo siento, esa función no está disponible por el momento. Sin embargo, puedo conectarte " +
        "con nuestro equipo de soporte especializado a través de estos canales:"

@Composable
fun OutOfScopeSupportCard(
    timeLabel: String,
    onWebClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.ChatBubbleAi),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = introBody,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.ChatTextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            OutOfScopeChannelRow(
                label = "Web",
                icon = Icons.Outlined.Language,
                onClick = onWebClick,
            )
            Spacer(Modifier.height(8.dp))
            OutOfScopeChannelRow(
                label = "Llamada",
                icon = Icons.Outlined.Phone,
                onClick = onCallClick,
            )
            Spacer(Modifier.height(8.dp))
            OutOfScopeChannelRow(
                label = "WhatsApp",
                icon = Icons.Outlined.Chat,
                onClick = onWhatsAppClick,
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.ChatTextMuted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun OutOfScopeChannelRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppPalette.LoginPrimaryOrange)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppPalette.LoginTextPrimary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.LoginTextPrimary,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = AppPalette.LoginTextPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}
