package com.bank.banking.api

import com.bank.banking.api.intent.IntentClassifierFactory
import com.bank.banking.application.usecase.BuildChatUiUseCase
import com.bank.banking.application.usecase.GetCurrentBalanceUseCase
import com.bank.banking.application.sdui.BalanceUiBuilder
import com.bank.banking.application.sdui.ChatUiBuilderFactory
import com.bank.banking.application.sdui.ClarificationUiBuilder
import com.bank.banking.application.sdui.GreetingUiBuilder
import com.bank.banking.application.sdui.NotImplementedTransferUiBuilder
import com.bank.banking.application.sdui.PayCreditCardUiBuilder
import com.bank.banking.application.sdui.SupportUiBuilder
import com.bank.banking.application.sdui.ChatHistoryUiBuilder
import com.bank.banking.application.sdui.MonthlyExpensesUiBuilder
import com.bank.banking.application.usecase.GetMonthlyExpensesByCategoryUseCase
import com.bank.banking.application.usecase.ListChatHistoryUseCase
import com.bank.banking.application.usecase.ListCreditCardMovementsUseCase
import com.bank.banking.application.usecase.ListCreditCardsWithDebtUseCase
import com.bank.banking.application.usecase.ListUserCardMovementsUseCase
import com.bank.banking.application.usecase.ListUserPaymentsUseCase
import com.bank.banking.application.usecase.LoginUseCase
import com.bank.banking.application.usecase.PayCreditCardUseCase
import com.bank.banking.application.usecase.RouteChatMessageUseCase
import com.bank.banking.infra.mongo.BcryptPasswordHasher
import com.bank.banking.infra.mongo.MongoAccountRepository
import com.bank.banking.infra.mongo.MongoBankingFactory
import com.bank.banking.infra.mongo.MongoBootstrap
import com.bank.banking.infra.mongo.MongoChatSessionRepository
import com.bank.banking.infra.mongo.MongoCreditCardRepository
import com.bank.banking.infra.mongo.MongoExpenseRepository
import com.bank.banking.infra.mongo.MongoIdempotencyRepository
import com.bank.banking.infra.mongo.MongoPaymentExecutionGateway
import com.bank.banking.infra.mongo.MongoPaymentHistoryRepository
import com.bank.banking.infra.mongo.MongoSessionRepository
import com.bank.banking.infra.mongo.MongoUserCredentialsRepository
import com.bank.banking.infra.mongo.MongoUserRepository
import com.bank.banking.infra.mongo.SecureTokenGeneratorImpl
import kotlinx.coroutines.runBlocking

/**
 * Ensambla infraestructura Mongo y casos de uso.
 */
class BankingCompositionRoot(connectionString: String, databaseName: String = "banking") {
    private val db = MongoBankingFactory.database(connectionString, databaseName)
    private val hasher = BcryptPasswordHasher()
    private val tokens = SecureTokenGeneratorImpl()

    val users = MongoUserRepository(db)
    val creds = MongoUserCredentialsRepository(db)
    val sessions = MongoSessionRepository(db)
    val accounts = MongoAccountRepository(db)
    val cards = MongoCreditCardRepository(db)
    val idempotency = MongoIdempotencyRepository(db)
    val paymentGateway = MongoPaymentExecutionGateway(db)
    val paymentHistory = MongoPaymentHistoryRepository(db)
    val chatSessions = MongoChatSessionRepository(db)
    val expenses = MongoExpenseRepository(db)

    val loginUseCase = LoginUseCase(users, creds, hasher, sessions, tokens)
    val balanceUseCase = GetCurrentBalanceUseCase(sessions, accounts)
    val cardsWithDebtUseCase = ListCreditCardsWithDebtUseCase(sessions, cards)
    val movementsUseCase = ListCreditCardMovementsUseCase(sessions, cards)
    val userCardMovementsUseCase = ListUserCardMovementsUseCase(sessions, cards)
    val payUseCase = PayCreditCardUseCase(sessions, accounts, cards, idempotency, paymentGateway, tokens)
    val userPaymentsUseCase = ListUserPaymentsUseCase(sessions, paymentHistory)
    val listChatHistoryUseCase = ListChatHistoryUseCase(sessions, chatSessions)
    val monthlyExpensesUseCase = GetMonthlyExpensesByCategoryUseCase(sessions, expenses)

    private val intentClassifier = IntentClassifierFactory.createFromEnvironment()
    val routeChatMessageUseCase = RouteChatMessageUseCase(intentClassifier)

    private val balanceUiBuilder = BalanceUiBuilder(balanceUseCase)
    private val payCreditCardUiBuilder = PayCreditCardUiBuilder(cardsWithDebtUseCase)
    private val clarificationUiBuilder = ClarificationUiBuilder()
    private val greetingUiBuilder = GreetingUiBuilder(balanceUseCase)
    private val supportUiBuilder = SupportUiBuilder()
    private val notImplementedTransferUiBuilder = NotImplementedTransferUiBuilder()
    private val chatHistoryUiBuilder = ChatHistoryUiBuilder(listChatHistoryUseCase)
    private val monthlyExpensesUiBuilder = MonthlyExpensesUiBuilder(monthlyExpensesUseCase)
    private val chatUiBuilderFactory = ChatUiBuilderFactory(
        balance = balanceUiBuilder,
        payCard = payCreditCardUiBuilder,
        chatHistory = chatHistoryUiBuilder,
        monthlyExpenses = monthlyExpensesUiBuilder,
        support = supportUiBuilder,
        clarification = clarificationUiBuilder,
        notImplementedTransfer = notImplementedTransferUiBuilder,
    )
    val buildChatUiUseCase = BuildChatUiUseCase(
        sessions = sessions,
        chatSessions = chatSessions,
        routeChatMessage = routeChatMessageUseCase,
        builderFactory = chatUiBuilderFactory,
        clarificationBuilder = clarificationUiBuilder,
        supportBuilder = supportUiBuilder,
        greetingBuilder = greetingUiBuilder,
    )

    fun bootstrapBlocking() {
        runBlocking {
            MongoBootstrap(db, hasher).run()
            sessions.ensureIndexes()
            chatSessions.ensureIndexes()
            expenses.ensureIndexes()
        }
    }
}
