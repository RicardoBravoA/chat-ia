package com.bank.banking.api

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

fun main() {
    val mongoUri = System.getenv("MONGO_URI") ?: "mongodb://127.0.0.1:27017"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    val root = BankingCompositionRoot(mongoUri)
    root.bootstrapBlocking()

    embeddedServer(Netty, port = port) {
        module(root)
    }.start(wait = true)
}

fun Application.module(root: BankingCompositionRoot) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(WebSockets)
    installBankingStatusPages()

    routing {
        bankingV1Routes(root)
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }
}
