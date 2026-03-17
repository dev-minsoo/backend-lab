package com.lab.redis.demo

import com.lab.redis.basics.BasicsSnapshot
import com.lab.redis.cache.ProductView
import com.lab.redis.leaderboard.LeaderboardEntry
import com.lab.redis.pubsub.LabEvent

data class RedisDemoSnapshot(
    val basicsSnapshot: BasicsSnapshot?,
    val cacheQueryCount: Int,
    val cachedProduct: ProductView?,
    val pubSubEvents: List<LabEvent>,
    val lockValue: String?,
    val inventoryStock: Int,
    val leaderboard: List<LeaderboardEntry>,
    val rateLimitCounter: String?,
    val recentActivities: List<RedisDemoActivity>,
)
