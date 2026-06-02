package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@Composable
fun HomeBalanceCard(
    balanceLabel: String,
    isLoading: Boolean,
    balanceVisible: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AppPalette.CardNavyTop, AppPalette.CardNavyBottom),
                ),
            ),
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(BalanceVisibilityTriangleSize),
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.2f),
                style = Fill,
            )
        }
        IconButton(
            onClick = onToggleBalanceVisibility,
            enabled = !isLoading,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 2.dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector = if (balanceVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (balanceVisible) "Ocultar saldo" else "Mostrar saldo",
                tint = Color.White,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SALDO DISPONIBLE",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (balanceVisible) balanceLabel else "••••••••",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val BalanceVisibilityTriangleSize = 76.dp
