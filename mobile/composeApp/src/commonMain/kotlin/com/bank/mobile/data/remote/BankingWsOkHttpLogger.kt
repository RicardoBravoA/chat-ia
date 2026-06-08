package com.bank.mobile.data.remote

import io.ktor.http.HttpHeaders

private const val MAX_BODY_CHARS = 16_000

/**
 * OkHttp-inspired logging for WebSocket text frames.
 * Ktor's [io.ktor.client.plugins.logging.Logging] plugin does not cover WS payloads.
 */
internal object BankingWsOkHttpLogger {

    fun logOutgoingTextFrame(
        url: String,
        body: String,
        hasBearerAuth: Boolean,
    ) {
        println("--> WS TEXT $url")
        if (hasBearerAuth) {
            println("${HttpHeaders.Authorization}: ***")
        }
        println("${HttpHeaders.ContentType}: application/json")
        println("")
        println(formatBody(body))
        println("--> END WS (${body.encodeToByteArray().size}-byte body)")
    }

    fun logIncomingTextFrame(
        url: String,
        body: String,
        tookMs: Long,
    ) {
        println("<-- WS TEXT $url (${tookMs}ms)")
        println(formatBody(body))
        println("<-- END WS (${body.encodeToByteArray().size}-byte body)")
    }
}

private fun formatBody(body: String): String =
    if (body.length > MAX_BODY_CHARS) {
        body.take(MAX_BODY_CHARS) + "… (${body.length} chars total)"
    } else {
        body
    }
