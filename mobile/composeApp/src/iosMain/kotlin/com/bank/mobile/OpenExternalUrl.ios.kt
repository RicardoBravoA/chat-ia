package com.bank.mobile

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Suppress("DEPRECATION")
actual fun openExternalUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
