package com.bank.mobile

import android.content.Context
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

internal object AndroidContextHolder {
    lateinit var applicationContext: Context
        private set

    private var activityRef: WeakReference<FragmentActivity>? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun attachActivity(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
    }

    fun detachActivity(activity: FragmentActivity) {
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    fun currentActivity(): FragmentActivity? = activityRef?.get()
}
