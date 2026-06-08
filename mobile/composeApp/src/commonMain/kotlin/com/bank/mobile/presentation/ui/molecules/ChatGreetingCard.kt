package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bank.mobile.domain.model.sdui.UiAction
import com.bank.mobile.presentation.ui.atoms.AppPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatGreetingCard(
    message: String,
    quickReplies: List<UiAction>,
    timeLabel: String,
    enabled: Boolean,
    onQuickReply: (label: String, selectedIntent: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistantMessageBubble(timeLabel = timeLabel) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.ChatTextPrimary,
            )
            if (quickReplies.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickReplies.forEach { action ->
                        val selectedIntent = action.payload["intent"]
                        AssistChip(
                            onClick = {
                                if (selectedIntent != null) {
                                    onQuickReply(action.label, selectedIntent)
                                }
                            },
                            enabled = enabled && action.actionType == "QUICK_REPLY" && selectedIntent != null,
                            label = { Text(action.label) },
                            shape = RoundedCornerShape(20.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFEFF6FF),
                                labelColor = AppPalette.PrimaryBlue,
                            ),
                        )
                    }
                }
            }
        }
    }
}
