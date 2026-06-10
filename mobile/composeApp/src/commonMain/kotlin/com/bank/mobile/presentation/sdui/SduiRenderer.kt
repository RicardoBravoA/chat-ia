package com.bank.mobile.presentation.sdui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardChatAction
import com.bank.mobile.domain.model.sdui.UiComponentType
import com.bank.mobile.domain.model.sdui.UiNode
import com.bank.mobile.openExternalUrl
import com.bank.mobile.presentation.support.SupportContactDefaults
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.molecules.AssistantMessageBubble
import com.bank.mobile.presentation.ui.molecules.ChatBalanceMiniCard
import com.bank.mobile.presentation.ui.molecules.ChatGreetingCard
import com.bank.mobile.presentation.ui.molecules.ChatHistoryRowCard
import com.bank.mobile.presentation.ui.molecules.OutOfScopeSupportCard
import com.bank.mobile.presentation.ui.molecules.SpendingCategoryRowCard
import com.bank.mobile.presentation.ui.organisms.PayCardChatPanel

@Composable
fun SduiRenderer(
    node: UiNode,
    timestampEpochMs: Long,
    payingCardId: String?,
    paidCardIds: Set<String> = emptySet(),
    onPayCard: (PayCardChatAction, CreditCardPaymentMode, Double?, sduiMessageKey: Long) -> Unit,
    onQuickReply: (label: String, selectedIntent: String) -> Unit = { _, _ -> },
    sduiMessageKey: Long = 0L,
    modifier: Modifier = Modifier,
) {
    when (node.type) {
        UiComponentType.COLUMN -> {
            Column(modifier = modifier) {
                node.children.forEach { child ->
                    SduiRenderer(
                        node = child,
                        timestampEpochMs = timestampEpochMs,
                        payingCardId = payingCardId,
                        paidCardIds = paidCardIds,
                        sduiMessageKey = sduiMessageKey,
                        onPayCard = onPayCard,
                        onQuickReply = onQuickReply,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        UiComponentType.ROW -> {
            Column(modifier = modifier) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    node.children.forEachIndexed { index, child ->
                        if (index > 0) {
                            Spacer(Modifier.width(8.dp))
                        }
                        SduiRenderer(
                            node = child,
                            timestampEpochMs = timestampEpochMs,
                            payingCardId = payingCardId,
                            paidCardIds = paidCardIds,
                            sduiMessageKey = sduiMessageKey,
                            onPayCard = onPayCard,
                            onQuickReply = onQuickReply,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        UiComponentType.ASSISTANT_TEXT -> {
            AssistantMessageBubble(timeLabel = formatTimeLabel(timestampEpochMs)) {
                Text(
                    node.propText("text"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.ChatTextPrimary,
                )
            }
        }
        UiComponentType.BALANCE_CARD -> {
            ChatBalanceMiniCard(amountLabel = node.propText("amountFormatted"))
        }
        UiComponentType.PAY_CARD_PANEL -> {
            val action = node.toPayCardChatAction()
            if (action != null) {
                PayCardChatPanel(
                    action = action,
                    paying = payingCardId == action.cardId,
                    paymentsDisabled = payingCardId != null && payingCardId != action.cardId,
                    paymentCompleted = action.cardId in paidCardIds,
                    onPay = { mode -> onPayCard(action, mode, null, sduiMessageKey) },
                )
            } else {
                SduiFallback(node.type)
            }
        }
        UiComponentType.SUPPORT_CHANNELS_CARD -> {
            OutOfScopeSupportCard(
                timeLabel = formatTimeLabel(timestampEpochMs),
                onWebClick = { openExternalUrl(SupportContactDefaults.supportWebUrl) },
                onCallClick = { openExternalUrl(SupportContactDefaults.supportPhoneDial) },
                onWhatsAppClick = { openExternalUrl(SupportContactDefaults.supportWhatsAppUrl) },
            )
        }
        UiComponentType.INFO_BANNER -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
            ) {
                Text(
                    node.propText("text"),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.ChatTextPrimary,
                )
            }
        }
        UiComponentType.GREETING_CARD -> {
            ChatGreetingCard(
                message = node.propText("message"),
                quickReplies = node.actions.filter { it.actionType == "QUICK_REPLY" },
                timeLabel = formatTimeLabel(timestampEpochMs),
                enabled = payingCardId == null,
                onQuickReply = onQuickReply,
            )
        }
        UiComponentType.CHAT_HISTORY_ROW -> {
            ChatHistoryRowCard(
                sessionLabel = node.propText("sessionLabel"),
                lastMessage = node.propText("lastMessage"),
                turnCount = node.propInt("turnCount") ?: 0,
                timeLabel = node.propText("timeLabel"),
                lastIntent = node.propText("lastIntent"),
            )
        }
        UiComponentType.SPENDING_CATEGORY_ROW -> {
            SpendingCategoryRowCard(
                category = node.propText("category"),
                transactionCount = node.propInt("transactionCount") ?: 0,
                totalAmountFormatted = node.propText("totalAmountFormatted"),
            )
        }
        else -> SduiFallback(node.type)
    }
}

@Composable
private fun SduiFallback(type: String) {
    AssistantMessageBubble(timeLabel = "") {
        Text(
            "Componente no soportado: $type",
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.ChatTextMuted,
        )
    }
}

private fun formatTimeLabel(timestampEpochMs: Long): String {
    if (timestampEpochMs <= 0L) return ""
    return com.bank.mobile.presentation.ui.atoms.formatChatTime12h(timestampEpochMs)
}
