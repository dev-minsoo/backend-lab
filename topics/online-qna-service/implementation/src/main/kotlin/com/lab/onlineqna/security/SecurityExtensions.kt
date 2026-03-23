package com.lab.onlineqna.security

import com.lab.onlineqna.exception.DomainException
import org.springframework.security.core.context.SecurityContextHolder

fun currentUserId(): Long {
    val principal = SecurityContextHolder.getContext().authentication?.principal as? JwtPrincipal
        ?: throw DomainException("인증이 필요합니다.")
    return principal.userId
}
