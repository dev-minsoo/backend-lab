package com.lab.redis.demo

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedDeque

@Service
class RedisDemoActivityLogService {
    private val logs = ConcurrentLinkedDeque<RedisDemoActivity>()

    fun append(
        title: String,
        summary: String,
        redisKey: String,
        redisCommand: String,
    ) {
        logs.addFirst(
            RedisDemoActivity(
                at = LocalDateTime.now().toString(),
                title = title,
                summary = summary,
                redisKey = redisKey,
                redisCommand = redisCommand,
            ),
        )
        while (logs.size > 12) {
            logs.removeLast()
        }
    }

    fun recent(): List<RedisDemoActivity> = logs.toList()

    fun clear() {
        logs.clear()
    }
}
