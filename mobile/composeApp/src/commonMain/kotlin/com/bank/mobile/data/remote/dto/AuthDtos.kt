package com.bank.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class LoginResponseDto(val token: String)
