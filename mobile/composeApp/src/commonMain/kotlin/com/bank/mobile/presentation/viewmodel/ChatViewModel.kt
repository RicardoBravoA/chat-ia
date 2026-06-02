package com.bank.mobile.presentation.viewmodel

import com.bank.mobile.domain.model.ChatBubble
import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PaymentReceiptUi
import com.bank.mobile.domain.model.PayCardChatAction
import com.bank.mobile.domain.usecase.PayCreditCardUseCase
import com.bank.mobile.domain.usecase.SendChatMessageUseCase
import com.bank.mobile.presentation.session.AuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock

data class ChatUiState(
    val messages: List<ChatBubble> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val payingCardId: String? = null,
)

class ChatViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val payCreditCardUseCase: PayCreditCardUseCase,
    private val authSession: AuthSession,
    private val onPaymentSuccess: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val welcomeBotMessage = welcomeMessage()
    private val _state = MutableStateFlow(ChatUiState(messages = listOf(welcomeBotMessage)))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun clear() {
        scope.cancel()
    }

    fun onDraftChange(value: String) {
        _state.update { it.copy(draft = value) }
    }

    fun sendChatMessage() {
        val text = _state.value.draft.trim()
        val token = authSession.currentToken() ?: return
        if (text.isBlank() || _state.value.sending) return

        scope.launch {
            val sentAt = nowMs()
            _state.updateOnMain {
                it.copy(
                    draft = "",
                    sending = true,
                    messages = ensureWelcomeMessage(it.messages, welcomeBotMessage) +
                        ChatBubble(text = text, isUser = true, timestampEpochMs = sentAt),
                )
            }
            delay(FRAME_DELAY_MS)
            yield()
            try {
                val response = sendChatMessageUseCase(token, text)
                val assistantAt = nowMs()
                val assistantBubble = ChatBubble(
                    text = "",
                    isUser = false,
                    sduiRoot = response.uiTree,
                    timestampEpochMs = assistantAt,
                )
                _state.updateOnMain {
                    it.copy(
                        messages = ensureWelcomeMessage(it.messages, welcomeBotMessage) + assistantBubble,
                    )
                }
            } catch (e: Throwable) {
                appendAssistantMessage("Error: ${e.message ?: "desconocido"}", welcomeBotMessage)
            } finally {
                _state.updateOnMain { it.copy(sending = false) }
            }
        }
    }

    fun payCreditCard(
        action: PayCardChatAction,
        mode: CreditCardPaymentMode,
        customAmount: Double?,
        sduiMessageKey: Long,
    ) {
        val token = authSession.currentToken() ?: return
        val sourceBubble = _state.value.messages.find { it.timestampEpochMs == sduiMessageKey } ?: return
        if (action.cardId in sourceBubble.paidCardIds) return
        scope.launch {
            _state.updateOnMain { it.copy(payingCardId = action.cardId) }
            delay(FRAME_DELAY_MS)
            yield()
            try {
                val result = payCreditCardUseCase(
                    token = token,
                    cardId = action.cardId,
                    cardholderName = action.cardholderName,
                    expiryMonth = action.expiryMonth,
                    expiryYear = action.expiryYear,
                    mode = mode,
                    customAmount = customAmount,
                )
                val receiptInstant = nowMs()
                val paidAmount =
                    kotlin.math.round((action.debt - result.newCardDebt) * 100.0) / 100.0
                val receipt = PaymentReceiptUi(
                    amountPaid = paidAmount.coerceAtLeast(0.0),
                    currency = result.currency,
                    movementId = result.movementId,
                    occurredAtEpochMs = receiptInstant,
                )
                val followUpAt = nowMs()
                _state.updateOnMain { state ->
                    state.copy(
                        messages = state.messages.map { bubble ->
                            if (bubble.timestampEpochMs == sduiMessageKey) {
                                bubble.copy(paidCardIds = bubble.paidCardIds + action.cardId)
                            } else {
                                bubble
                            }
                        } +
                            ChatBubble(
                                text = "",
                                isUser = false,
                                paymentReceipt = receipt,
                                timestampEpochMs = receiptInstant,
                            ) +
                            ChatBubble(
                                text = "¡Pago realizado con éxito! Tu tarjeta ha sido actualizada.\n\n¿Necesitas algo más?",
                                isUser = false,
                                timestampEpochMs = followUpAt,
                            ),
                    )
                }
                onPaymentSuccess()
            } catch (e: Throwable) {
                appendAssistantMessage(e.message ?: "No se pudo completar el pago.", welcomeBotMessage)
            } finally {
                _state.updateOnMain { it.copy(payingCardId = null) }
            }
        }
    }

    private suspend fun appendAssistantMessage(text: String, welcome: ChatBubble) {
        val at = nowMs()
        _state.updateOnMain {
            it.copy(
                messages = ensureWelcomeMessage(it.messages, welcome) +
                    ChatBubble(text = text, isUser = false, timestampEpochMs = at),
            )
        }
    }

    private fun ensureWelcomeMessage(messages: List<ChatBubble>, welcome: ChatBubble): List<ChatBubble> {
        return if (messages.any { !it.isUser && it.text == welcome.text }) {
            messages
        } else {
            listOf(welcome) + messages
        }
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()
}

private fun welcomeMessage(): ChatBubble = ChatBubble(
    text = "Hola, ¿En qué puedo ayudarte hoy?",
    isUser = false,
    timestampEpochMs = Clock.System.now().toEpochMilliseconds(),
)

private const val FRAME_DELAY_MS = 32L
