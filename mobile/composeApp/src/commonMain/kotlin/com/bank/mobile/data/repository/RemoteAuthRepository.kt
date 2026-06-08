package com.bank.mobile.data.repository

import com.bank.mobile.data.remote.dto.LoginRequestDto
import com.bank.mobile.data.remote.dto.LoginResponseDto
import com.bank.mobile.domain.repository.AuthRepository
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class RemoteAuthRepository(
    private val deps: BankingRemoteDependencies,
) : AuthRepository {

    override suspend fun login(email: String, password: String): String {
        val url = "${deps.baseUrl}/v1/auth/login"
        val response = deps.client.post {
            url(url)
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email, password))
        }
        val body = response.bodyAsText()
        return deps.json.decodeFromString<LoginResponseDto>(body).token
    }
}
