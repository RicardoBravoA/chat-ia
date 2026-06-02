package com.bank.mobile.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.presentation.ui.atoms.AppPalette
import com.bank.mobile.presentation.ui.molecules.HomeBalanceCard
import com.bank.mobile.presentation.ui.molecules.HomeMovementsSectionHeader
import com.bank.mobile.presentation.ui.molecules.PaymentHistoryCard

@Composable
fun HomeScreen(
    balanceLabel: String,
    payments: List<Payment>,
    paymentsLoading: Boolean,
    paymentsError: String?,
    onRefreshPayments: () -> Unit,
    onSeeAllMovements: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var balanceVisible by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppPalette.HomeScreenBackground),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HomeBalanceCard(
                    balanceLabel = balanceLabel,
                    isLoading = paymentsLoading,
                    balanceVisible = balanceVisible,
                    onToggleBalanceVisibility = { balanceVisible = !balanceVisible },
                )
            }
            item {
                HomeMovementsSectionHeader(onSeeAllClick = onSeeAllMovements)
            }
            if (!paymentsLoading) {
                when {
                    paymentsError != null -> item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(paymentsError, color = MaterialTheme.colorScheme.error)
                            Button(
                                onClick = onRefreshPayments,
                                colors = ButtonDefaults.buttonColors(containerColor = AppPalette.LoginPrimaryOrange),
                            ) { Text("Reintentar") }
                        }
                    }
                    payments.isEmpty() -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, shape = MaterialTheme.shapes.medium)
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No hay movimientos aún.", color = AppPalette.LoginTextMuted)
                        }
                    }
                    else -> items(payments) { p -> PaymentHistoryCard(p) }
                }
            }
        }
    }
}
