package com.bank.mobile.presentation.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Mutaciones de UI disparadas desde corrutinas (red, etc.). */
internal suspend fun <T> MutableStateFlow<T>.updateOnMain(block: (T) -> T) {
    withContext(Dispatchers.Main.immediate) {
        update(block)
    }
}
