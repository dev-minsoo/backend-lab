package com.lab.redis.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/demo/redis")
class RedisDemoApiController(
    private val actionService: RedisDemoActionService,
) {
    @GetMapping("/snapshot")
    fun snapshot(): RedisDemoSnapshot = actionService.snapshot()

    @PostMapping("/basics/populate")
    fun populateBasics(): RedisDemoResponse = actionService.populateBasics()

    @PostMapping("/cache/run")
    fun runCacheDemo(): RedisDemoResponse = actionService.runCacheDemo()

    @PostMapping("/cache/evict")
    fun evictCache(): RedisDemoResponse = actionService.evictCache()

    @PostMapping("/pubsub/publish")
    fun publishPubSubEvent(): RedisDemoResponse = actionService.publishPubSubEvent()

    @PostMapping("/lock/simple")
    fun simpleLockDemo(): RedisDemoResponse = actionService.simpleLockDemo()

    @PostMapping("/lock/redisson")
    fun redissonLockDemo(): RedisDemoResponse = actionService.redissonLockDemo()

    @PostMapping("/leaderboard/seed")
    fun seedLeaderboard(): RedisDemoResponse = actionService.seedLeaderboard()

    @PostMapping("/ratelimit/hit")
    fun hitRateLimit(): RedisDemoResponse = actionService.hitRateLimit()

    @PostMapping("/reset")
    fun reset(): RedisDemoResponse = actionService.reset()
}
