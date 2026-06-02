package com.bank.mobile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bank.mobile.data.repository.BankingRemoteDependencies
import com.bank.mobile.data.repository.RemoteAuthRepository
import com.bank.mobile.data.repository.RemoteHomeRepository
import com.bank.mobile.data.repository.RemotePaymentsRepository
import com.bank.mobile.domain.usecase.GetBalanceUseCase
import com.bank.mobile.domain.usecase.GetPaymentsUseCase
import com.bank.mobile.domain.usecase.LoginUseCase
import com.bank.mobile.domain.usecase.PayCreditCardUseCase
import com.bank.mobile.domain.usecase.SendChatMessageUseCase
import com.bank.mobile.presentation.ui.atoms.formatMoney
import com.bank.mobile.presentation.ui.molecules.HomeTopBar
import com.bank.mobile.presentation.navigation.BankNavRoutes
import com.bank.mobile.presentation.navigation.BankingNavHost
import com.bank.mobile.presentation.navigation.SessionNavigationEffect
import com.bank.mobile.presentation.session.AuthSession
import com.bank.mobile.presentation.viewmodel.ChatViewModel
import com.bank.mobile.presentation.viewmodel.HomeViewModel
import com.bank.mobile.presentation.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankingApp() {
    val navController = rememberNavController()
    val remoteDeps = remember { BankingRemoteDependencies.create() }
    val authRepository = remember(remoteDeps) { RemoteAuthRepository(remoteDeps) }
    val homeRepository = remember(remoteDeps) { RemoteHomeRepository(remoteDeps) }
    val paymentsRepository = remember(remoteDeps) { RemotePaymentsRepository(remoteDeps) }

    val loginUseCase = remember { LoginUseCase(authRepository) }
    val getBalanceUseCase = remember { GetBalanceUseCase(homeRepository) }
    val sendChatMessageUseCase = remember { SendChatMessageUseCase(homeRepository) }
    val getPaymentsUseCase = remember { GetPaymentsUseCase(paymentsRepository) }
    val payCreditCardUseCase = remember { PayCreditCardUseCase(homeRepository) }

    val authSession = remember { AuthSession() }
    val homeVm = remember {
        HomeViewModel(
            getBalanceUseCase = getBalanceUseCase,
            getPaymentsUseCase = getPaymentsUseCase,
            authSession = authSession,
            formatMoney = ::formatMoney,
        )
    }
    val chatVm = remember {
        ChatViewModel(
            sendChatMessageUseCase = sendChatMessageUseCase,
            payCreditCardUseCase = payCreditCardUseCase,
            authSession = authSession,
            onPaymentSuccess = { homeVm.refreshHomeData() },
        )
    }
    val loginVm = remember {
        LoginViewModel(
            loginUseCase = loginUseCase,
            authSession = authSession,
            onLoginSuccess = { homeVm.refreshHomeData() },
        )
    }

    val sessionState by authSession.sessionState.collectAsState()
    val loginState by loginVm.state.collectAsState()
    val homeState by homeVm.state.collectAsState()
    val chatState by chatVm.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    DisposableEffect(Unit) {
        onDispose {
            loginVm.clear()
            homeVm.clear()
            chatVm.clear()
        }
    }

    SessionNavigationEffect(sessionState = sessionState, navController = navController)

    MaterialTheme {
        Surface {
            Scaffold(
                topBar = {
                    if (currentRoute == BankNavRoutes.Home) {
                        HomeTopBar(nickname = homeState.userNickname)
                    }
                },
            ) { pad ->
                BankingNavHost(
                    navController = navController,
                    loginState = loginState,
                    onEmailChange = loginVm::onEmailChange,
                    onPasswordChange = loginVm::onPasswordChange,
                    onLoginClick = loginVm::login,
                    balanceLabel = homeState.balanceLabel,
                    payments = homeState.movements,
                    paymentsLoading = homeState.movementsLoading,
                    paymentsError = homeState.movementsError,
                    onRefreshPayments = homeVm::refreshHomeData,
                    chatMessages = chatState.messages,
                    chatDraft = chatState.draft,
                    onChatDraftChange = chatVm::onDraftChange,
                    onSendChat = chatVm::sendChatMessage,
                    chatSending = chatState.sending,
                    onPayCard = { action, mode, custom, key ->
                        chatVm.payCreditCard(action, mode, custom, key)
                    },
                    payingCardId = chatState.payingCardId,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad),
                )
            }
        }
    }
}
