package com.bank.mobile.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardChatAction
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.atoms.formatMoney
import com.bank.mobile.presentation.ui.molecules.CreditCardChatVisual
import com.bank.mobile.presentation.ui.molecules.PaymentModeOptionBox

@Composable
fun PayCardChatPanel(
    action: PayCardChatAction,
    paying: Boolean,
    paymentsDisabled: Boolean,
    paymentCompleted: Boolean = false,
    onPay: (CreditCardPaymentMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(action.cardId) { mutableStateOf(CreditCardPaymentMode.FULL_DEBT) }
    val interactionsEnabled = !paymentCompleted && !paymentsDisabled

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        CreditCardChatVisual(action)
        Spacer(Modifier.height(16.dp))
        Text(
            "DEUDA ACTUAL",
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.ChatTextMuted,
            letterSpacing = 1.sp,
        )
        Text(
            formatMoney(action.debt, action.currency),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppPalette.ChatTextPrimary,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PaymentModeOptionBox(
                label = "PAGO MÍNIMO",
                amount = formatMoney(action.minimumPaymentDue, action.currency),
                selected = selected == CreditCardPaymentMode.MINIMUM,
                modifier = Modifier.weight(1f),
                enabled = interactionsEnabled,
                onClick = { selected = CreditCardPaymentMode.MINIMUM },
            )
            PaymentModeOptionBox(
                label = "PAGO TOTAL",
                amount = formatMoney(action.debt, action.currency),
                selected = selected == CreditCardPaymentMode.FULL_DEBT,
                modifier = Modifier.weight(1f),
                enabled = interactionsEnabled,
                onClick = { selected = CreditCardPaymentMode.FULL_DEBT },
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPay(selected) },
            enabled = interactionsEnabled && !paying,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (paymentCompleted) {
                    AppPalette.ChatTextMuted
                } else {
                    AppPalette.AccentOrange
                },
                contentColor = Color.White,
                disabledContainerColor = AppPalette.ChatTextMuted.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.9f),
            ),
        ) {
            when {
                paying -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                }
                paymentCompleted -> {
                    Text("Pago realizado", fontWeight = FontWeight.SemiBold)
                }
                else -> {
                    Text("Pagar ahora", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
