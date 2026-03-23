package com.lab.onlineqna.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val nickname: String,
    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String
)

data class LoginRequest(
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val user: UserSummary
)
