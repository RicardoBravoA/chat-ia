package com.bank.banking.api

import com.bank.banking.api.dto.BalanceResponse
import com.bank.banking.api.dto.ChatRouteRequest
import com.bank.banking.api.dto.ChatMessageRequest
import com.bank.banking.api.dto.ChatWsEnvelope
import com.bank.banking.api.dto.LoginRequest
import com.bank.banking.api.dto.LoginResponse
import com.bank.banking.api.dto.PayCardRequest
import com.bank.banking.api.dto.PayCardResponse
import com.bank.banking.api.dto.ErrorResponse
import com.bank.banking.application.usecase.PayCreditCardCommand
import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.error.InvalidPaymentAmountException
import com.bank.banking.domain.model.CreditCardPaymentMode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.random.Random

internal fun Route.bankingV1Routes(root: BankingCompositionRoot) {
    val wsJson = Json { ignoreUnknownKeys = true }
    route("/v1") {
        post("/auth/login") {
            val body = call.receive<LoginRequest>()
            val token = root.loginUseCase.execute(body.email, body.password)
            call.respond(LoginResponse(token = token.value))
        }

        post("/chat/route") {
            call.requireSession(root)
            val body = call.receive<ChatRouteRequest>()
            val result = root.routeChatMessageUseCase.execute(body.message)
            call.respond(result.toResponse())
        }

        webSocket("/chat/ws") {
            val token = try {
                call.requireSession(root)
            } catch (_: Throwable) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }

            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                try {
                    val text = frame.readText()
                    val req = wsJson.decodeFromString(ChatMessageRequest.serializer(), text)
                    val waitMs = Random.nextLong(1_000, 3_001)
                    delay(waitMs)
                    val result = root.buildChatUiUseCase.execute(token, req.message).toResponse()
                    val envelope = ChatWsEnvelope(
                        event = "chat_response",
                        response = result,
                    )
                    outgoing.send(Frame.Text(wsJson.encodeToString(ChatWsEnvelope.serializer(), envelope)))
                } catch (e: Throwable) {
                    val errorEnvelope = ChatWsEnvelope(
                        event = "chat_error",
                        error = ErrorResponse(code = "CHAT_WS_ERROR", message = e.message ?: "Invalid chat payload"),
                    )
                    outgoing.send(Frame.Text(wsJson.encodeToString(ChatWsEnvelope.serializer(), errorEnvelope)))
                }
            }
        }

        get("/me/balance") {
            val t = call.requireBearerToken()
            val result = root.balanceUseCase.execute(t)
            call.respond(
                BalanceResponse(
                    amount = result.balance.amount,
                    currency = result.balance.currency,
                    nickname = result.nickname,
                ),
            )
        }

        get("/credit-cards/with-debt") {
            val t = call.requireBearerToken()
            val withDebt = root.cardsWithDebtUseCase.execute(t)
            if (withDebt.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(withDebt.map { it.toCreditCardWithDebtItem() })
            }
        }

        get("/credit-cards/{cardId}/movements") {
            val t = call.requireBearerToken()
            val cardId = call.cardIdParameter()
            val list = root.movementsUseCase.execute(t, cardId)
            call.respond(list.map { it.toMovementItem() })
        }

        get("/me/movements") {
            val t = call.requireBearerToken()
            val list = root.userCardMovementsUseCase.execute(t)
            call.respond(list.map { it.toMovementItem() })
        }

        get("/payments") {
            val t = call.requireBearerToken()
            val list = root.userPaymentsUseCase.execute(t)
            call.respond(list.map { it.toPaymentItem() })
        }

        post("/credit-cards/{cardId}/payments") {
            val t = call.requireBearerToken()
            val cardId = call.cardIdParameter()
            val idem = call.request.headers["Idempotency-Key"]
                ?: throw InvalidPaymentAmountException("Idempotency-Key header required")
            val body = call.receive<PayCardRequest>()
            val mode = try {
                CreditCardPaymentMode.valueOf(body.paymentMode)
            } catch (_: IllegalArgumentException) {
                throw BadRequestException("Invalid paymentMode")
            }
            val result = root.payUseCase.execute(
                token = t,
                cardId = cardId,
                idempotencyKey = idem,
                command = PayCreditCardCommand(
                    cardholderName = body.cardholderName,
                    expiryMonth = body.expiryMonth,
                    expiryYear = body.expiryYear,
                    mode = mode,
                    customAmount = body.amount,
                ),
            )
            call.respond(
                PayCardResponse(
                    newAccountAmount = result.newAccountBalance.amount,
                    newCardDebt = result.newCardDebt.amount,
                    currency = result.newAccountBalance.currency,
                    movementId = result.movementId,
                ),
            )
        }
    }
}
