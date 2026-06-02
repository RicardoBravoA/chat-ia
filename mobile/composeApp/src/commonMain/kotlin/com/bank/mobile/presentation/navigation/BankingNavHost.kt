package com.bank.mobile.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bank.mobile.domain.model.ChatBubble
import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardChatAction
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.presentation.screen.ChatScreen
import com.bank.mobile.presentation.screen.LoginScreen
import com.bank.mobile.presentation.ui.organisms.HomeWithChatFab
import com.bank.mobile.presentation.viewmodel.LoginUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankingNavHost(
    navController: NavHostController,
    loginState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    balanceLabel: String,
    payments: List<Payment>,
    paymentsLoading: Boolean,
    paymentsError: String?,
    onRefreshPayments: () -> Unit,
    chatMessages: List<ChatBubble>,
    chatDraft: String,
    onChatDraftChange: (String) -> Unit,
    onSendChat: () -> Unit,
    chatSending: Boolean,
    onPayCard: (PayCardChatAction, CreditCardPaymentMode, Double?, Long) -> Unit,
    payingCardId: String?,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BankNavRoutes.Login,
        modifier = modifier,
    ) {
        composable(BankNavRoutes.Login) {
            LoginScreen(
                email = loginState.email,
                password = loginState.password,
                error = loginState.error,
                isLoading = loginState.loading,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onLoginClick = onLoginClick,
            )
        }
        composable(BankNavRoutes.Home) {
            HomeWithChatFab(
                onOpenChat = { navController.navigate(BankNavRoutes.Chat) },
                balanceLabel = balanceLabel,
                payments = payments,
                paymentsLoading = paymentsLoading,
                paymentsError = paymentsError,
                onRefreshPayments = onRefreshPayments,
            )
        }
        composable(BankNavRoutes.Chat) {
            SystemBackHandler(enabled = true) {
                navController.popBackStack()
            }
            ChatScreen(
                messages = chatMessages,
                draft = chatDraft,
                onDraftChange = onChatDraftChange,
                onSend = onSendChat,
                isSending = chatSending,
                onPayCard = onPayCard,
                payingCardId = payingCardId,
                onBackToHome = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
