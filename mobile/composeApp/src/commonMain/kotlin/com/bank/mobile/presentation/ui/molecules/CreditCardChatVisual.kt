package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.bank.mobile.domain.model.PayCardChatAction
import com.bank.mobile.presentation.ui.atoms.AppPalette

/**
 * Tarjeta de crédito en el hilo de chat (datos del [PayCardChatAction]).
 */
@Composable
fun CreditCardChatVisual(
    action: PayCardChatAction,
    modifier: Modifier = Modifier,
) {
    val brandLine = action.alias.uppercase().ifBlank { "PLATINUM" }
    val lastFour = action.lastFourDigits
    val holder = action.cardholderName.uppercase()
    val exp = "${action.expiryMonth.toString().padStart(2, '0')}/" +
        (action.expiryYear % 100).toString().padStart(2, '0')

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AppPalette.CardNavyTop, AppPalette.CardNavyBottom),
                ),
            )
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    brandLine,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "VISA",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
            }
            Text(
                "NFC",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "••••   ••••   ••••   $lastFour",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "CARD HOLDER",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    holder,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "EXPIRES",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    exp,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
