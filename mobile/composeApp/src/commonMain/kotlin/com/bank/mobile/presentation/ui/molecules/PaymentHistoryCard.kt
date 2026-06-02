package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.atoms.formatHomeMovementDateTime
import com.bank.mobile.presentation.ui.atoms.formatMoney
import com.bank.mobile.presentation.ui.atoms.paymentMovementIcon
import kotlin.math.abs

@Composable
fun PaymentHistoryCard(p: Payment) {
    val isCharge = p.status.equals("CHARGE", ignoreCase = true)
    val isPaid = p.status.equals("PAID", ignoreCase = true)
    val amountColor =
        when {
            isCharge -> AppPalette.HomeAmountExpense
            isPaid -> AppPalette.PrimaryBlue
            else -> AppPalette.LoginTextMuted
        }
    val sign =
        when {
            isCharge -> "- "
            isPaid -> "+ "
            else -> ""
        }
    val amountLabel = formatMoney(abs(p.amount), p.currency)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.HomeTransactionCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppPalette.HomeMovementIconBox),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = paymentMovementIcon(p),
                        contentDescription = null,
                        tint = AppPalette.PrimaryBlue,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.description.ifBlank { p.status },
                        color = AppPalette.LoginTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatHomeMovementDateTime(p.occurredAtEpochMs),
                        color = AppPalette.LoginTextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
            Text(
                text = "$sign$amountLabel",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                textAlign = TextAlign.End,
            )
        }
    }
}
