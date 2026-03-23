package com.lab.onlineqna.service

import com.lab.onlineqna.domain.User
import com.lab.onlineqna.dto.AuthResponse
import com.lab.onlineqna.dto.LoginRequest
import com.lab.onlineqna.dto.SignUpRequest
import com.lab.onlineqna.exception.DomainException
import com.lab.onlineqna.repository.UserRepository
import com.lab.onlineqna.security.JwtTokenProvider
import com.lab.onlineqna.support.toSummary
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun signUp(request: SignUpRequest): AuthResponse {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw DomainException("이미 가입된 이메일입니다.")
        }

        val user = userRepository.save(
            User(
                email = request.email,
                nickname = request.nickname,
                password = passwordEncoder.encode(request.password)
            )
        )
        return issueToken(user)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { DomainException("이메일 또는 비밀번호가 올바르지 않습니다.") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw DomainException("이메일 또는 비밀번호가 올바르지 않습니다.")
        }
        return issueToken(user)
    }

    private fun issueToken(user: User): AuthResponse = AuthResponse(
        accessToken = jwtTokenProvider.createToken(user.id!!, user.email, "ROLE_${user.role.name}"),
        user = user.toSummary()
    )
}
