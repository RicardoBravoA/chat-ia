package com.bank.banking.api

import com.bank.banking.api.dto.ErrorResponse
import com.bank.banking.domain.error.AuthenticationException
import com.bank.banking.domain.error.BadRequestException
import com.bank.banking.domain.error.ConflictException
import com.bank.banking.domain.error.DomainException
import com.bank.banking.domain.error.InsufficientFundsException
import com.bank.banking.domain.error.InvalidPaymentAmountException
import com.bank.banking.domain.error.NotFoundException
import com.bank.banking.domain.error.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.installBankingStatusPages() {
    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(code = "AUTHENTICATION_FAILED", message = cause.message ?: "Invalid credentials"),
            )
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(code = "UNAUTHORIZED", message = cause.message ?: "Unauthorized"),
            )
        }
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(code = "NOT_FOUND", message = cause.message ?: "Not found"),
            )
        }
        exception<ConflictException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(code = "CONFLICT", message = cause.message ?: "Conflict"),
            )
        }
        exception<InsufficientFundsException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "INSUFFICIENT_FUNDS", message = cause.message ?: "Insufficient funds"),
            )
        }
        exception<InvalidPaymentAmountException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "INVALID_PAYMENT", message = cause.message ?: "Invalid payment"),
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "BAD_REQUEST", message = cause.message ?: "Bad request"),
            )
        }
        exception<DomainException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "DOMAIN_ERROR", message = cause.message ?: "Error"),
            )
        }
    }
}
