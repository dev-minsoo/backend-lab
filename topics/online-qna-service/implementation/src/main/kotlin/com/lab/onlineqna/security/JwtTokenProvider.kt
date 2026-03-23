package com.lab.onlineqna.security

import com.lab.onlineqna.config.AppProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Component
class JwtTokenProvider(
    private val appProperties: AppProperties
) {

    private val key = Keys.hmacShaKeyFor(appProperties.jwt.secret.toByteArray(StandardCharsets.UTF_8))

    fun createToken(userId: Long, email: String, role: String): String {
        val now = Instant.now()
        val expiry = now.plus(appProperties.jwt.accessTokenExpirationMinutes, ChronoUnit.MINUTES)
        return Jwts.builder()
            .issuer(appProperties.jwt.issuer)
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact()
    }

    fun getAuthentication(token: String): UsernamePasswordAuthenticationToken {
        val claims = parseClaims(token)
        return UsernamePasswordAuthenticationToken(
            JwtPrincipal(
                userId = claims.subject.toLong(),
                email = claims["email"] as String
            ),
            token,
            listOf(SimpleGrantedAuthority(claims["role"] as String))
        )
    }

    fun validate(token: String): Boolean {
        runCatching { parseClaims(token) }.getOrElse { return false }
        return true
    }

    private fun parseClaims(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}

data class JwtPrincipal(
    val userId: Long,
    val email: String
)
