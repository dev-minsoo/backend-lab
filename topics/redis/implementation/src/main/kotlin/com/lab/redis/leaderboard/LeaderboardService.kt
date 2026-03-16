package com.lab.redis.leaderboard

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

data class LeaderboardEntry(
    val member: String,
    val score: Double,
)

@Service
class LeaderboardService(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    private val key = "leaderboard:study"

    fun recordScore(member: String, score: Double) {
        stringRedisTemplate.opsForZSet().add(key, member, score)
    }

    fun top(limit: Long): List<LeaderboardEntry> =
        stringRedisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, limit - 1)
            .orEmpty()
            .mapNotNull { tuple ->
                val member = tuple.value ?: return@mapNotNull null
                val score = tuple.score ?: return@mapNotNull null
                LeaderboardEntry(member, score)
            }

    fun rankOf(member: String): Long? = stringRedisTemplate.opsForZSet().reverseRank(key, member)
}
