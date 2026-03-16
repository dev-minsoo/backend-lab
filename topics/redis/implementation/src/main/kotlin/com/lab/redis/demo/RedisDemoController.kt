package com.lab.redis.demo

import com.lab.redis.basics.RedisBasicsService
import com.lab.redis.cache.ProductCatalogService
import com.lab.redis.leaderboard.LeaderboardService
import com.lab.redis.lock.RedissonInventoryLockService
import com.lab.redis.lock.SimpleRedisLockService
import com.lab.redis.pubsub.LabEvent
import com.lab.redis.pubsub.LabEventPublisher
import com.lab.redis.pubsub.LabEventSubscriber
import com.lab.redis.ratelimit.RateLimitService
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Controller
@RequestMapping("/demo/redis")
class RedisDemoController(
    private val redisBasicsService: RedisBasicsService,
    private val productCatalogService: ProductCatalogService,
    private val publisher: LabEventPublisher,
    private val subscriber: LabEventSubscriber,
    private val simpleRedisLockService: SimpleRedisLockService,
    private val redissonInventoryLockService: RedissonInventoryLockService,
    private val leaderboardService: LeaderboardService,
    private val rateLimitService: RateLimitService,
    private val snapshotService: RedisDemoSnapshotService,
    private val stringRedisTemplate: StringRedisTemplate,
    private val cacheManager: CacheManager,
) {
    @GetMapping
    fun page(model: Model): String {
        model.addAttribute("snapshot", snapshotService.snapshot())
        return "redis-demo"
    }

    @PostMapping("/basics/populate")
    fun populateBasics(redirectAttributes: RedirectAttributes): String {
        val snapshot = redisBasicsService.populateAndRead()
        redirectAttributes.addFlashAttribute(
            "message",
            "기본 자료구조 데이터를 채웠습니다. TTL=${snapshot.ttlSeconds}s, Top=${snapshot.sortedTop.joinToString()}",
        )
        return "redirect:/demo/redis"
    }

    @PostMapping("/cache/run")
    fun runCacheDemo(redirectAttributes: RedirectAttributes): String {
        productCatalogService.resetCounter()
        val first = productCatalogService.getProduct(1L)
        val second = productCatalogService.getProduct(1L)
        redirectAttributes.addFlashAttribute(
            "message",
            "캐시 데모 완료. 첫 조회 loadedAt=${first.loadedAtEpochMillis}, 두 번째 조회 loadedAt=${second.loadedAtEpochMillis}, queryCount=${productCatalogService.queryCount()}",
        )
        return "redirect:/demo/redis"
    }

    @PostMapping("/cache/evict")
    fun evictCache(redirectAttributes: RedirectAttributes): String {
        productCatalogService.evictProduct(1L)
        redirectAttributes.addFlashAttribute("message", "products 캐시에서 product:1 을 제거했습니다.")
        return "redirect:/demo/redis"
    }

    @PostMapping("/pubsub/publish")
    fun publishPubSubEvent(redirectAttributes: RedirectAttributes): String {
        val payload = "study-session-${LocalDateTime.now()}"
        publisher.publish(LabEvent(type = "study", payload = payload))
        val deadline = System.currentTimeMillis() + 1_000
        while (subscriber.receivedEvents().none { it.payload == payload } && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        redirectAttributes.addFlashAttribute("message", "Pub/Sub 이벤트를 발행했습니다: $payload")
        return "redirect:/demo/redis"
    }

    @PostMapping("/lock/simple")
    fun simpleLockDemo(redirectAttributes: RedirectAttributes): String {
        val first = simpleRedisLockService.acquire("lock:demo", Duration.ofSeconds(5))
        val second = simpleRedisLockService.acquire("lock:demo", Duration.ofSeconds(5))
        val released = if (first != null) simpleRedisLockService.release("lock:demo", first) else false

        redirectAttributes.addFlashAttribute(
            "message",
            "단순 락 결과: first=${first != null}, second=${second != null}, released=$released",
        )
        return "redirect:/demo/redis"
    }

    @PostMapping("/lock/redisson")
    fun redissonLockDemo(redirectAttributes: RedirectAttributes): String {
        redissonInventoryLockService.resetStock(5)
        val executor = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(20)

        repeat(20) {
            executor.submit {
                try {
                    redissonInventoryLockService.decreaseWithLock()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdownNow()

        redirectAttributes.addFlashAttribute(
            "message",
            "Redisson 락 데모 완료. 20개 요청 후 stock=${redissonInventoryLockService.currentStock()}",
        )
        return "redirect:/demo/redis"
    }

    @PostMapping("/leaderboard/seed")
    fun seedLeaderboard(redirectAttributes: RedirectAttributes): String {
        leaderboardService.recordScore("alice", 120.0)
        leaderboardService.recordScore("charlie", 111.0)
        leaderboardService.recordScore("bob", 99.0)
        redirectAttributes.addFlashAttribute("message", "리더보드 점수를 적재했습니다.")
        return "redirect:/demo/redis"
    }

    @PostMapping("/ratelimit/hit")
    fun hitRateLimit(redirectAttributes: RedirectAttributes): String {
        val result = rateLimitService.allow("demo-user")
        redirectAttributes.addFlashAttribute(
            "message",
            "Rate Limit 결과: allowed=${result.allowed}, current=${result.currentCount}",
        )
        return "redirect:/demo/redis"
    }

    @PostMapping("/reset")
    fun reset(redirectAttributes: RedirectAttributes): String {
        stringRedisTemplate.connectionFactory!!.connection.serverCommands().flushAll()
        cacheManager.getCache("products")?.clear()
        subscriber.clear()
        productCatalogService.resetCounter()
        redissonInventoryLockService.resetStock(5)
        redirectAttributes.addFlashAttribute("message", "Redis 상태와 데모 상태를 초기화했습니다.")
        return "redirect:/demo/redis"
    }
}
