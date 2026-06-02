package com.bank.mobile.presentation.ui.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.graphics.vector.ImageVector
import com.bank.mobile.domain.model.Payment

/** Icono representativo según descripción y tipo de movimiento (Material). */
fun paymentMovementIcon(p: Payment): ImageVector {
    val d = p.description.lowercase()
    val isCharge = p.status.equals("CHARGE", ignoreCase = true)
    val isPaid = p.status.equals("PAID", ignoreCase = true)
    return when {
        isPaid -> Icons.Outlined.AccountBalanceWallet
        d.contains("netflix") || d.contains("spotify") || d.contains("stream") -> Icons.Outlined.Tv
        d.contains("starbucks") || d.contains("restaurant") || d.contains("café") || d.contains("cafe") ->
            Icons.Outlined.Restaurant
        d.contains("amazon") || d.contains("compra") || d.contains("shop") -> Icons.Outlined.ShoppingBag
        isCharge -> Icons.Outlined.ShoppingBag
        else -> Icons.Outlined.CreditCard
    }
}
