package com.bank.mobile.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val bankingJson = Json { ignoreUnknownKeys = true }

internal fun createBankingHttpClient(json: Json = bankingJson): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }
