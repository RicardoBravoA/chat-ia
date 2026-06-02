package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@Composable
fun LoginSubmitButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = AppPalette.LoginPrimaryOrange.copy(alpha = 0.45f),
                ambientColor = AppPalette.LoginPrimaryOrange.copy(alpha = 0.28f),
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppPalette.LoginPrimaryOrange,
            contentColor = Color.White,
            disabledContainerColor = AppPalette.LoginPrimaryOrangeLoading,
            disabledContentColor = Color.White,
        ),
    ) {
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ButtonInlineLoadingArc()
                Text("Ingresando…", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ingresar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}
