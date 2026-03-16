package com.lab.redis.ratelimit

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

data class RateLimitResult(
    val allowed: Boolean,
    val currentCount: Long,
)

@Service
class RateLimitService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val properties: RateLimitProperties,
) {
    fun allow(clientId: String): RateLimitResult {
        val key = "rate-limit:$clientId"
        val current = stringRedisTemplate.opsForValue().increment(key) ?: 0L
        if (current == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(properties.fixedWindowSeconds))
        }
        return RateLimitResult(
            allowed = current <= properties.maxRequests,
            currentCount = current,
        )
    }
}
