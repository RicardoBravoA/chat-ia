package com.bank.mobile

/**
 * Abre un URI externo: https, tel:, o enlaces tipo [https://wa.me/...](https://wa.me/...).
 */
expect fun openExternalUrl(url: String)
