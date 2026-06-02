package com.bank.mobile.presentation.ui.molecules

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bank.mobile.presentation.ui.atoms.AppPalette

/**
 * Arco girando con [Canvas] + [InfiniteTransition]: en KMP el CircularProgressIndicator
 * indeterminado a veces no anima; este dibujo sí rota en todas las plataformas.
 */
@Composable
private fun RotatingArcLoader(
    modifier: Modifier = Modifier.size(52.dp),
    arcColor: Color = AppPalette.AccentOrange,
    trackColor: Color = AppPalette.PrimaryBlue.copy(alpha = 0.12f),
    strokeWidth: Dp = 5.dp,
) {
    val transition = rememberInfiniteTransition(label = "loading_arc")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    Canvas(
        modifier = modifier.rotate(rotation),
    ) {
        val sw = strokeWidth.toPx()
        val diameter = size.minDimension - sw
        val topLeft = Offset(sw / 2f, sw / 2f)
        val arcSize = Size(diameter, diameter)
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = arcColor,
            startAngle = -90f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

/** Indicador compacto para botón (p. ej. login): anima en KMP donde CircularProgressIndicator falla. */
@Composable
fun ButtonInlineLoadingArc(
    modifier: Modifier = Modifier,
) {
    RotatingArcLoader(
        modifier = modifier.size(20.dp),
        arcColor = Color.White,
        trackColor = Color.White.copy(alpha = 0.38f),
        strokeWidth = 2.dp,
    )
}

/**
 * Scrim oscuro + tarjeta blanca opaca: el progreso circular suele perderse sobre fondos
 * semitransparentes sin contraste; aquí el indicador siempre tiene fondo claro fijo.
 */
@Composable
fun FullScreenLoadingOverlay(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(Color.Black.copy(alpha = 0.52f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 10.dp,
            color = Color.White,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RotatingArcLoader()
                Text(
                    text = message,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
