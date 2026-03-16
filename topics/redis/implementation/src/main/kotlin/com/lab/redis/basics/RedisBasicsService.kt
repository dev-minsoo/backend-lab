package com.lab.redis.basics

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

data class BasicsSnapshot(
    val stringValue: String?,
    val hashValue: String?,
    val setMembers: Set<String>,
    val listValues: List<String>,
    val sortedTop: List<String>,
    val ttlSeconds: Long,
)

@Service
class RedisBasicsService(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    fun populateAndRead(): BasicsSnapshot {
        stringRedisTemplate.opsForValue().set("basics:string", "redis-is-in-memory", Duration.ofMinutes(5))
        stringRedisTemplate.opsForHash<String, String>().put("basics:hash", "thread-model", "single-threaded-event-loop")
        stringRedisTemplate.opsForSet().add("basics:set", "cache", "pubsub", "lock")
        stringRedisTemplate.opsForList().rightPushAll("basics:list", listOf("learn", "measure", "compare"))
        stringRedisTemplate.opsForZSet().add("basics:zset", "alice", 120.0)
        stringRedisTemplate.opsForZSet().add("basics:zset", "bob", 95.0)

        return BasicsSnapshot(
            stringValue = stringRedisTemplate.opsForValue().get("basics:string"),
            hashValue = stringRedisTemplate.opsForHash<String, String>().get("basics:hash", "thread-model"),
            setMembers = stringRedisTemplate.opsForSet().members("basics:set").orEmpty(),
            listValues = stringRedisTemplate.opsForList().range("basics:list", 0, -1).orEmpty(),
            sortedTop = stringRedisTemplate.opsForZSet().reverseRange("basics:zset", 0, 1).orEmpty().toList(),
            ttlSeconds = stringRedisTemplate.getExpire("basics:string"),
        )
    }
}
