package com.lab.redis.demo

import com.lab.redis.basics.BasicsSnapshot
import com.lab.redis.cache.ProductCatalogService
import com.lab.redis.cache.ProductView
import com.lab.redis.leaderboard.LeaderboardService
import com.lab.redis.lock.RedissonInventoryLockService
import com.lab.redis.pubsub.LabEventSubscriber
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisDemoSnapshotService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val productCatalogService: ProductCatalogService,
    private val subscriber: LabEventSubscriber,
    private val redissonInventoryLockService: RedissonInventoryLockService,
    private val leaderboardService: LeaderboardService,
    private val activityLogService: RedisDemoActivityLogService,
) {
    fun snapshot(): RedisDemoSnapshot {
        return RedisDemoSnapshot(
            basicsSnapshot = readBasicsSnapshot(),
            cacheQueryCount = productCatalogService.queryCount(),
            cachedProduct = readCachedProduct(),
            pubSubEvents = subscriber.receivedEvents(),
            lockValue = stringRedisTemplate.opsForValue().get("lock:demo"),
            inventoryStock = redissonInventoryLockService.currentStock(),
            leaderboard = leaderboardService.top(5),
            rateLimitCounter = stringRedisTemplate.opsForValue().get("rate-limit:demo-user"),
            recentActivities = activityLogService.recent(),
        )
    }

    private fun readBasicsSnapshot(): BasicsSnapshot? {
        val stringValue = stringRedisTemplate.opsForValue().get("basics:string") ?: return null
        val hashValue = stringRedisTemplate.opsForHash<String, String>().get("basics:hash", "thread-model")
        val setMembers = stringRedisTemplate.opsForSet().members("basics:set").orEmpty()
        val listValues = stringRedisTemplate.opsForList().range("basics:list", 0, -1).orEmpty()
        val sortedTop = stringRedisTemplate.opsForZSet().reverseRange("basics:zset", 0, 1).orEmpty().toList()
        val ttlSeconds = stringRedisTemplate.getExpire("basics:string")

        return BasicsSnapshot(
            stringValue = stringValue,
            hashValue = hashValue,
            setMembers = setMembers,
            listValues = listValues,
            sortedTop = sortedTop,
            ttlSeconds = ttlSeconds,
        )
    }

    private fun readCachedProduct(): ProductView? = productCatalogService.cachedProduct(1L)
}
