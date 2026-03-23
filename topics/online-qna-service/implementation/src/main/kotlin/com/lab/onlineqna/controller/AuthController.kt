package com.lab.onlineqna.controller

import com.lab.onlineqna.dto.AuthResponse
import com.lab.onlineqna.dto.LoginRequest
import com.lab.onlineqna.dto.SignUpRequest
import com.lab.onlineqna.service.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): AuthResponse = authService.signUp(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse = authService.login(request)
}
