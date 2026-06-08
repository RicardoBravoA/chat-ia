package com.bank.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidContextHolder.init(this)
        AndroidContextHolder.attachActivity(this)
        setContent { BankingApp() }
    }

    override fun onDestroy() {
        AndroidContextHolder.detachActivity(this)
        super.onDestroy()
    }
}
