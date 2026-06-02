package com.bank.mobile.presentation.ui.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bank.mobile.presentation.ui.atoms.AppPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    nickname: String,
    modifier: Modifier = Modifier,
) {
    val display = nickname.ifBlank { "Usuario" }
    val initial = display.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    CenterAlignedTopAppBar(
        modifier = modifier,
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, AppPalette.PrimaryBlue, CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = AppPalette.PrimaryBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = display,
                    color = AppPalette.PrimaryBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        actions = {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = AppPalette.PrimaryBlue,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(26.dp),
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
