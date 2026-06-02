package com.bank.mobile.presentation.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.GallerySaver
import com.bank.mobile.domain.model.PaymentReceiptUi
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.atoms.formatMoney
import com.bank.mobile.presentation.ui.atoms.formatReceiptDate
import com.bank.mobile.presentation.ui.atoms.formatReceiptReference
import kotlinx.coroutines.launch

@Composable
fun PaymentSuccessReceiptCard(
    receipt: PaymentReceiptUi,
    modifier: Modifier = Modifier,
    onSaveResult: (Result<Unit>) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val amountLabel = formatMoney(receipt.amountPaid, receipt.currency)
    val ref = formatReceiptReference(receipt.movementId)
    val dateStr = formatReceiptDate(receipt.occurredAtEpochMs)
    Column(modifier = modifier.widthIn(max = 320.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(AppPalette.CardNavyBottom),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "✓",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Pago Exitoso",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppPalette.ChatTextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "COMPROBANTE DE OPERACIÓN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppPalette.ChatTextPrimary,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "MONTO PAGADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppPalette.ChatTextPrimary,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            amountLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppPalette.PrimaryBlue,
                            textAlign = TextAlign.End,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "REFERENCIA",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.ChatTextMuted,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                ref,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.ChatTextPrimary,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                "FECHA",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppPalette.ChatTextMuted,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.ChatTextPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val r = GallerySaver.savePaymentReceiptImage(receipt)
                                onSaveResult(r)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPalette.AccentOrange,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Guardar imagen en galería", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
