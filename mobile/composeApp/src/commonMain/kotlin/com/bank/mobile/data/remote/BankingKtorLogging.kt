package com.bank.mobile.data.remote

import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders

private val PASSWORD_JSON = Regex(""""password"\s*:\s*"[^"]*"""")

internal val bankingHttpLogger: Logger =
    object : Logger {
        override fun log(message: String) {
            println(redactSensitiveHttpLog(message))
        }
    }

internal val bankingHttpLogLevel: LogLevel = LogLevel.BODY

internal fun shouldSanitizeHttpHeader(header: String): Boolean =
    header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
        header.equals("Idempotency-Key", ignoreCase = true)

private fun redactSensitiveHttpLog(message: String): String =
    message.replace(PASSWORD_JSON, """"password":"***"""")
