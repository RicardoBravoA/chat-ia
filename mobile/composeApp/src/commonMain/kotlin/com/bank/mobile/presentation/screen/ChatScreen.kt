package com.bank.mobile.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.domain.model.ChatBubble
import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardChatAction
import com.bank.mobile.presentation.sdui.SduiRenderer
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.atoms.formatChatTime12h
import com.bank.mobile.presentation.ui.molecules.AssistantMessageBubble
import com.bank.mobile.presentation.ui.molecules.ChatAssistantHeader
import com.bank.mobile.presentation.ui.molecules.ChatTypingDots
import com.bank.mobile.presentation.ui.organisms.PaymentSuccessReceiptCard
import com.bank.mobile.showShortMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatBubble>,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    onPayCard: (PayCardChatAction, CreditCardPaymentMode, Double?, Long) -> Unit = { _, _, _, _ -> },
    payingCardId: String? = null,
    onBackToHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty() || isSending) {
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) {
                listState.animateScrollToItem(last)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppPalette.ChatBackground,
        topBar = {
            if (onBackToHome != null) {
                ChatAssistantHeader(onClose = onBackToHome)
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Message...",
                                    color = AppPalette.ChatTextMuted.copy(alpha = 0.8f),
                                )
                            },
                            shape = RoundedCornerShape(26.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                        )
                        Spacer(Modifier.size(10.dp))
                        val canSend = draft.isNotBlank() && !isSending
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) AppPalette.LoginPrimaryOrange
                                    else AppPalette.LoginPrimaryOrange.copy(alpha = 0.38f),
                                )
                                .clickable(enabled = canSend, onClick = onSend),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "➤",
                                color = Color(0xFF0F172A),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 3.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(AppPalette.ChatBackground),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 320.dp),
                            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start,
                        ) {
                            when {
                                !msg.isUser && msg.sduiRoot != null -> {
                                    SduiRenderer(
                                        node = msg.sduiRoot,
                                        timestampEpochMs = msg.timestampEpochMs,
                                        payingCardId = payingCardId,
                                        paidCardIds = msg.paidCardIds,
                                        sduiMessageKey = msg.timestampEpochMs,
                                        onPayCard = onPayCard,
                                    )
                                }
                                else -> {
                                    msg.paymentReceipt?.let { receipt ->
                                        PaymentSuccessReceiptCard(
                                            receipt = receipt,
                                            onSaveResult = { r ->
                                                if (r.isSuccess) {
                                                    showShortMessage("Imagen guardada en galería")
                                                } else {
                                                    showShortMessage(
                                                        r.exceptionOrNull()?.message
                                                            ?: "No se pudo guardar la imagen",
                                                    )
                                                }
                                            },
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    if (msg.text.isNotBlank()) {
                                        if (msg.isUser) {
                                            UserMessageBubble(msg)
                                        } else {
                                            AssistantMessageBubble(
                                                timeLabel = formatChatTime12h(msg.timestampEpochMs),
                                            ) {
                                                Text(
                                                    msg.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = AppPalette.ChatTextPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (isSending) {
                    item(key = "typing_indicator") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            ChatTypingDots()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(msg: ChatBubble) {
    Card(
        shape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 4.dp,
        ),
        colors = CardDefaults.cardColors(containerColor = AppPalette.PrimaryBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                msg.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
            Text(
                formatChatTime12h(msg.timestampEpochMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 6.dp),
            )
        }
    }
}
