package com.bank.mobile

import android.os.Handler
import android.os.Looper
import android.widget.Toast

actual fun showShortMessage(text: String) {
    val context = AndroidContextHolder.applicationContext
    val show = {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
        show()
    } else {
        Handler(Looper.getMainLooper()).post { show() }
    }
}
