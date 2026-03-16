package com.lab.redis.lock

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class SimpleRedisLockService(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    fun acquire(lockName: String, ttl: Duration): String? {
        val token = UUID.randomUUID().toString()
        val acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockName, token, ttl)
        return if (acquired == true) token else null
    }

    fun release(lockName: String, token: String): Boolean {
        val currentValue = stringRedisTemplate.opsForValue().get(lockName)
        if (currentValue != token) {
            return false
        }
        stringRedisTemplate.delete(lockName)
        return true
    }
}
