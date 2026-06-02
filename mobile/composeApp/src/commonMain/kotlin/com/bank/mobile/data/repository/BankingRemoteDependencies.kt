package com.bank.mobile.data.repository

import com.bank.mobile.backendBaseUrl
import com.bank.mobile.data.remote.bankingJson
import com.bank.mobile.data.remote.createBankingHttpClient
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Recursos HTTP compartidos: un solo [HttpClient] y [Json] para todos los repositorios remotos.
 */
class BankingRemoteDependencies(
    val client: HttpClient,
    val json: Json,
    val baseUrl: String,
) {
    companion object {
        fun create(): BankingRemoteDependencies {
            val json = bankingJson
            return BankingRemoteDependencies(
                client = createBankingHttpClient(json),
                json = json,
                baseUrl = backendBaseUrl(),
            )
        }
    }
}
