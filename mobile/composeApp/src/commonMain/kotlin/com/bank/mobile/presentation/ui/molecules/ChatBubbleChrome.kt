package com.bank.mobile.presentation.ui.molecules

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.atoms.formatChatTime12h
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AssistantMessageBubble(
    timeLabel: String,
    showTimestamp: Boolean = true,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 18.dp,
        ),
        colors = CardDefaults.cardColors(containerColor = AppPalette.ChatBubbleAi),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            content()
            if (showTimestamp) {
                Text(
                    timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppPalette.ChatTextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
fun ChatBubbleTimeFooter(
    epochMs: Long,
    alignEnd: Boolean,
) {
    Text(
        formatChatTime12h(epochMs),
        style = MaterialTheme.typography.labelSmall,
        color = AppPalette.ChatTextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
    )
}

@Composable
fun ChatTypingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typing_phase",
    )
    val dotCount = remember(phase) { ((phase * 3f).toInt() % 3) + 1 }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val t = phase.toDouble()
            val wave = sin(t * 2.0 * PI).toFloat()
            val dotsAlpha = 0.45f + 0.55f * ((wave + 1f) / 2f)
            Text(
                text = ".".repeat(dotCount),
                modifier = Modifier.alpha(dotsAlpha),
                style = MaterialTheme.typography.bodyLarge,
                color = AppPalette.ChatTextMuted,
            )
        }
    }
}
