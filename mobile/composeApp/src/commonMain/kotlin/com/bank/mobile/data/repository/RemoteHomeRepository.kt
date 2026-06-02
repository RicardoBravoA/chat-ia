package com.bank.mobile.data.repository

import com.bank.mobile.data.mapper.toDomain
import com.bank.mobile.data.mapper.toPayment
import com.bank.mobile.data.remote.dto.BalanceResponseDto
import com.bank.mobile.data.remote.dto.ChatMessageRequestDto
import com.bank.mobile.data.remote.dto.ChatWsEnvelopeDto
import com.bank.mobile.data.remote.dto.MovementItemDto
import com.bank.mobile.data.remote.dto.PayCardRequestDto
import com.bank.mobile.data.remote.dto.PayCardResponseDto
import com.bank.mobile.data.remote.logIncomingHttpResponse
import com.bank.mobile.data.remote.logOutgoingHttpRequest
import com.bank.mobile.data.remote.newBankingIdempotencyKey
import com.bank.mobile.data.remote.simulateBankingNetworkDelay
import com.bank.mobile.domain.model.Balance
import com.bank.mobile.domain.model.CreditCardPaymentMode
import com.bank.mobile.domain.model.PayCardPaymentResult
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.domain.model.sdui.ChatUiResponse
import com.bank.mobile.domain.repository.HomeRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.http.encodedPath
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText

class RemoteHomeRepository(
    private val deps: BankingRemoteDependencies,
) : HomeRepository {

    override suspend fun getBalance(token: String): Balance {
        simulateBankingNetworkDelay()
        val url = "${deps.baseUrl}/v1/me/balance"
        logOutgoingHttpRequest(
            method = "GET",
            url = url,
            hasBearerAuth = true,
            contentTypeJson = false,
            bodyDescription = null,
        )
        val response = deps.client.get {
            url(url)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body = response.bodyAsText()
        logIncomingHttpResponse("GET", url, response, body)
        val dto = deps.json.decodeFromString<BalanceResponseDto>(body)
        return dto.toDomain()
    }

    override suspend fun sendChatMessage(token: String, message: String): ChatUiResponse {
        val wsUrl = "${deps.baseUrl}/v1/chat/ws"
        val payload = deps.json.encodeToString(ChatMessageRequestDto.serializer(), ChatMessageRequestDto(message))
        logOutgoingHttpRequest(
            method = "WS",
            url = wsUrl,
            hasBearerAuth = true,
            contentTypeJson = true,
            bodyDescription = payload,
        )
        val session = deps.client.webSocketSession {
            url {
                takeFrom(wsUrl)
                protocol = if (protocol == URLProtocol.HTTPS) URLProtocol.WSS else URLProtocol.WS
                encodedPath = "/v1/chat/ws"
            }
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        try {
            session.outgoing.send(Frame.Text(payload))
            val incoming = session.incoming.receive() as? Frame.Text
                ?: error("No se recibió respuesta del chat en tiempo real")
            val body = incoming.readText()
            val envelope = deps.json.decodeFromString(ChatWsEnvelopeDto.serializer(), body)
            if (envelope.event == "chat_error") {
                error(envelope.error?.message ?: "Error de chat en websocket")
            }
            val response = envelope.response ?: error("Respuesta websocket inválida")
            return response.toDomain()
        } finally {
            session.close()
        }
    }

    override suspend fun payCreditCard(
        token: String,
        cardId: String,
        cardholderName: String,
        expiryMonth: Int,
        expiryYear: Int,
        mode: CreditCardPaymentMode,
        customAmount: Double?,
    ): PayCardPaymentResult {
        simulateBankingNetworkDelay()
        val url = "${deps.baseUrl}/v1/credit-cards/$cardId/payments"
        val idemKey = newBankingIdempotencyKey()
        val bodyDto = PayCardRequestDto(
            cardholderName = cardholderName,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            paymentMode = mode.name,
            amount = if (mode == CreditCardPaymentMode.CUSTOM) customAmount else null,
        )
        logOutgoingHttpRequest(
            method = "POST",
            url = url,
            hasBearerAuth = true,
            contentTypeJson = true,
            bodyDescription = "PayCardRequest(mode=${mode.name})",
            extraHeaderLines = listOf("Idempotency-Key: ***"),
        )
        val response = deps.client.post {
            url(url)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            header("Idempotency-Key", idemKey)
            setBody(deps.json.encodeToString(PayCardRequestDto.serializer(), bodyDto))
        }
        val body = response.bodyAsText()
        logIncomingHttpResponse("POST", url, response, body)
        if (response.status != HttpStatusCode.OK) {
            val hint = if (body.isNotBlank()) body else "HTTP ${response.status.value}"
            error("No se pudo completar el pago: $hint")
        }
        val dto = deps.json.decodeFromString<PayCardResponseDto>(body)
        return dto.toDomain()
    }

    override suspend fun getCreditCardMovements(token: String, cardId: String): List<Payment> {
        simulateBankingNetworkDelay()
        val url = "${deps.baseUrl}/v1/credit-cards/$cardId/movements"
        logOutgoingHttpRequest(
            method = "GET",
            url = url,
            hasBearerAuth = true,
            contentTypeJson = false,
            bodyDescription = null,
        )
        val response = deps.client.get {
            url(url)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body = response.bodyAsText()
        logIncomingHttpResponse("GET", url, response, body)
        if (response.status != HttpStatusCode.OK) {
            val hint = if (body.isNotBlank()) body else "HTTP ${response.status.value}"
            error("No se pudieron cargar los movimientos: $hint")
        }
        val rows = deps.json.decodeFromString<List<MovementItemDto>>(body)
        return rows.map { it.toPayment() }
    }
}
