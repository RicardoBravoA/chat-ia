package com.bank.mobile

import android.widget.Toast

actual fun showShortMessage(text: String) {
    Toast.makeText(AndroidContextHolder.applicationContext, text, Toast.LENGTH_SHORT).show()
}
