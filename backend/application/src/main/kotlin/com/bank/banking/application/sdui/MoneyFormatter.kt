package com.bank.banking.application.sdui

import com.bank.banking.domain.model.Money

internal fun Money.formatForDisplay(): String {
    val rounded = kotlin.math.round(amount * 100.0) / 100.0
    return "$rounded $currency"
}
