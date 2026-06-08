package com.bank.banking.application.sdui

import com.bank.banking.domain.model.IntentLabel

class ChatUiBuilderFactory(
    private val balance: BalanceUiBuilder,
    private val payCard: PayCreditCardUiBuilder,
    private val chatHistory: ChatHistoryUiBuilder,
    private val monthlyExpenses: MonthlyExpensesUiBuilder,
    private val support: SupportUiBuilder,
    private val clarification: ClarificationUiBuilder,
    private val notImplementedTransfer: NotImplementedTransferUiBuilder,
) {
    fun forIntent(intent: IntentLabel): ChatUiBuilder = when (intent) {
        IntentLabel.CHECK_BALANCE -> balance
        IntentLabel.PAY_CREDIT_CARD -> payCard
        IntentLabel.VIEW_CHAT_HISTORY -> chatHistory
        IntentLabel.MONTHLY_EXPENSES -> monthlyExpenses
        IntentLabel.TRANSFER_OWN_ACCOUNTS, IntentLabel.TRANSFER_THIRD_PARTY -> notImplementedTransfer
        IntentLabel.OUT_OF_SCOPE -> support
        IntentLabel.AMBIGUOUS -> clarification
    }
}
