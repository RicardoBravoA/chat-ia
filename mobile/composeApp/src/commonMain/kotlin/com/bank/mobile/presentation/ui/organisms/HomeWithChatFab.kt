package com.bank.mobile.presentation.ui.organisms

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.presentation.screen.HomeScreen
import com.bank.mobile.presentation.ui.atoms.AppPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWithChatFab(
    onOpenChat: () -> Unit,
    balanceLabel: String,
    payments: List<Payment>,
    paymentsLoading: Boolean,
    paymentsError: String?,
    onRefreshPayments: () -> Unit,
    onSeeAllMovements: () -> Unit = {},
) {
    var chatFabOffset by remember { mutableStateOf(Offset.Zero) }
    var chatFabInitialized by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fabSizePx = with(density) { FabSize.toPx() }
        val marginPx = with(density) { FabEdgeMargin.toPx() }
        val maxX = (constraints.maxWidth.toFloat() - fabSizePx - marginPx).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight.toFloat() - fabSizePx - marginPx).coerceAtLeast(0f)
        LaunchedEffect(maxX, maxY) {
            if (!chatFabInitialized) {
                chatFabOffset = Offset(maxX, maxY)
                chatFabInitialized = true
            } else {
                chatFabOffset = Offset(
                    x = chatFabOffset.x.coerceIn(0f, maxX),
                    y = chatFabOffset.y.coerceIn(0f, maxY),
                )
            }
        }

        HomeScreen(
            balanceLabel = balanceLabel,
            payments = payments,
            paymentsLoading = paymentsLoading,
            paymentsError = paymentsError,
            onRefreshPayments = onRefreshPayments,
            onSeeAllMovements = onSeeAllMovements,
            modifier = Modifier.fillMaxSize(),
        )
        FloatingActionButton(
            onClick = onOpenChat,
            modifier = Modifier
                .size(FabSize)
                .offset {
                    IntOffset(
                        x = chatFabOffset.x.roundToInt(),
                        y = chatFabOffset.y.roundToInt(),
                    )
                }
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        chatFabOffset = Offset(
                            x = (chatFabOffset.x + dragAmount.x).coerceIn(0f, maxX),
                            y = (chatFabOffset.y + dragAmount.y).coerceIn(0f, maxY),
                        )
                    }
                },
            shape = RoundedCornerShape(16.dp),
            containerColor = AppPalette.LoginPrimaryOrange,
            contentColor = Color.White,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = "Abrir chat",
            )
        }
    }
}

private val FabSize = 56.dp
private val FabEdgeMargin = 16.dp
