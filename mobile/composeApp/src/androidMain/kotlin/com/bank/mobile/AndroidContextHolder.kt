package com.bank.mobile

import android.content.Context

internal object AndroidContextHolder {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
