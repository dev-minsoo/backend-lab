package com.lab.redis.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.rate-limit")
data class RateLimitProperties(
    val fixedWindowSeconds: Long = 5,
    val maxRequests: Long = 3,
)
