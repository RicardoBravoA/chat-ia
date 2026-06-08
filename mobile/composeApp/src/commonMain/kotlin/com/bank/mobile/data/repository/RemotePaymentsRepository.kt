package com.bank.mobile.data.repository

import com.bank.mobile.data.mapper.toPayment
import com.bank.mobile.data.remote.dto.MovementItemDto
import com.bank.mobile.domain.model.Payment
import com.bank.mobile.domain.repository.PaymentsRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class RemotePaymentsRepository(
    private val deps: BankingRemoteDependencies,
) : PaymentsRepository {

    override suspend fun getPayments(token: String): List<Payment> {
        val url = "${deps.baseUrl}/v1/me/movements"
        val response = deps.client.get {
            url(url)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body = response.bodyAsText()
        if (response.status != HttpStatusCode.OK) {
            val hint = if (body.isNotBlank()) body else "HTTP ${response.status.value}"
            error("No se pudieron cargar los movimientos: $hint")
        }
        val rows = deps.json.decodeFromString<List<MovementItemDto>>(body)
        return rows.map { it.toPayment() }
    }
}
