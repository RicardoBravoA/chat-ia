package com.bank.mobile.presentation.ui.atoms

import androidx.compose.ui.graphics.Color

/** Tokens de color (átomos visuales). */
object AppPalette {
    val PrimaryBlue = Color(0xFF3D77FF)
    val AccentOrange = Color(0xFFFF961F)
    /** Fondo del botón de login mientras carga (`enabled = false` usa este tono). */
    val AccentOrangeLoading = Color(0xFFFFA733)

    /** Pantalla de login (tarjeta sobre fondo gris). */
    val LoginScreenBackground = Color(0xFFE8EAED)
    val LoginInputBackground = Color(0xFFF2F3F5)
    /** Botón principal según diseño (#FF7020). */
    val LoginPrimaryOrange = Color(0xFFFF7020)
    val LoginPrimaryOrangeLoading = Color(0xFFFF8A4A)
    val LoginTextPrimary = Color(0xFF0F172A)
    val LoginTextMuted = Color(0xFF64748B)
    val LoginLabel = Color(0xFF475569)
    /** Texto estático “¿olvidaste…?” (sin acción). */
    val LoginForgotPasswordDecorative = Color(0xFFD3543A)

    /** Chat (asistente) */
    val ChatBackground = Color(0xFFF0F2F5)
    val ChatBubbleAi = Color(0xFFE8EAED)
    val ChatBubbleUser = Color(0xFFFFFFFF)
    val ChatTextPrimary = Color(0xFF0F172A)
    val ChatTextMuted = Color(0xFF64748B)
    val ChatHeaderBar = Color(0xFFFFFFFF)
    /** Indicador “en línea” junto al título del chat (verde). */
    val OnlineDot = Color(0xFF22C55E)
    val CardNavyTop = Color(0xFF1E3A5F)
    val CardNavyBottom = Color(0xFF0D1B2A)

    /** Inicio: fondo general y tarjeta de movimiento. */
    val HomeScreenBackground = Color(0xFFF3F4F6)
    val HomeTransactionCard = Color(0xFFFFFFFF)
    /** Fondo del recuadro del icono en cada movimiento. */
    val HomeMovementIconBox = Color(0xFFE8EEF5)
    val HomeAmountExpense = Color(0xFF0F172A)

    val ErrorRed = Color(0xFFDC2626)
}
