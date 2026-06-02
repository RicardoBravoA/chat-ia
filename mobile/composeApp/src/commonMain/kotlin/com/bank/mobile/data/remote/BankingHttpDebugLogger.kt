package com.bank.mobile.data.remote

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

private const val MAX_LOG_BODY_CHARS = 16_000

internal fun logOutgoingHttpRequest(
    method: String,
    url: String,
    hasBearerAuth: Boolean,
    contentTypeJson: Boolean,
    bodyDescription: String?,
    extraHeaderLines: List<String> = emptyList(),
) {
    println("HTTP REQUEST $method $url")
    println("HTTP REQUEST HEADERS:")
    if (contentTypeJson) {
        println("  ${HttpHeaders.ContentType}: application/json")
    }
    if (hasBearerAuth) {
        println("  ${HttpHeaders.Authorization}: Bearer ***")
    }
    extraHeaderLines.forEach { println("  $it") }
    if (bodyDescription != null) {
        println("HTTP REQUEST BODY: $bodyDescription")
    } else {
        println("HTTP REQUEST BODY: (none)")
    }
}

internal fun logIncomingHttpResponse(
    method: String,
    url: String,
    response: HttpResponse,
    body: String,
    logResponseBody: Boolean = true,
) {
    println("HTTP RESPONSE $method $url -> ${response.status}")
    println("HTTP RESPONSE HEADERS:")
    response.headers.names().sorted().forEach { name ->
        val values = response.headers.getAll(name) ?: emptyList()
        val joined = values.joinToString(", ")
        val display =
            if (name.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                name.equals(HttpHeaders.WWWAuthenticate, ignoreCase = true)
            ) {
                "***"
            } else {
                joined
            }
        println("  $name: $display")
    }
    if (!logResponseBody) {
        println("HTTP RESPONSE BODY: (omitido)")
        return
    }
    val bodyLine =
        if (body.length > MAX_LOG_BODY_CHARS) {
            body.take(MAX_LOG_BODY_CHARS) + "… (${body.length} bytes total)"
        } else {
            body
        }
    println("HTTP RESPONSE BODY: $bodyLine")
}
