package com.lab.redis.integration

import com.lab.redis.basics.RedisBasicsService
import com.lab.redis.cache.ProductCatalogService
import com.lab.redis.leaderboard.LeaderboardService
import com.lab.redis.lock.RedissonInventoryLockService
import com.lab.redis.lock.SimpleRedisLockService
import com.lab.redis.pubsub.LabEvent
import com.lab.redis.pubsub.LabEventPublisher
import com.lab.redis.pubsub.LabEventSubscriber
import com.lab.redis.ratelimit.RateLimitService
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RedisLabTest : RedisIntegrationTestSupport() {
    @Autowired
    private lateinit var redisBasicsService: RedisBasicsService

    @Autowired
    private lateinit var productCatalogService: ProductCatalogService

    @Autowired
    private lateinit var publisher: LabEventPublisher

    @Autowired
    private lateinit var subscriber: LabEventSubscriber

    @Autowired
    private lateinit var simpleRedisLockService: SimpleRedisLockService

    @Autowired
    private lateinit var redissonInventoryLockService: RedissonInventoryLockService

    @Autowired
    private lateinit var leaderboardService: LeaderboardService

    @Autowired
    private lateinit var rateLimitService: RateLimitService

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Test
    @DisplayName("Redis 기본 자료구조를 사용해 값을 저장하고 읽는다")
    fun `should demonstrate redis data structures`() {
        val snapshot = redisBasicsService.populateAndRead()

        snapshot.stringValue shouldBe "redis-is-in-memory"
        snapshot.hashValue shouldBe "single-threaded-event-loop"
        snapshot.setMembers shouldContain "cache"
        snapshot.listValues shouldBe listOf("learn", "measure", "compare")
        snapshot.sortedTop.first() shouldBe "alice"
        snapshot.ttlSeconds.shouldBeGreaterThan(0)
    }

    @Test
    @DisplayName("캐시 히트가 발생하면 원본 조회 횟수가 증가하지 않는다")
    fun `should cache product lookup`() {
        productCatalogService.resetCounter()

        val first = productCatalogService.getProduct(1L)
        val second = productCatalogService.getProduct(1L)

        first.id shouldBe second.id
        productCatalogService.queryCount() shouldBe 1
        cacheManager.getCache("products")!!.get(1L).shouldNotBeNull()
    }

    @Test
    @DisplayName("Pub Sub으로 발행한 이벤트를 구독자가 수신한다")
    fun `should receive pub sub event`() {
        subscriber.clear()

        publisher.publish(LabEvent(type = "study", payload = "redis-pubsub"))

        val deadline = System.currentTimeMillis() + 3_000
        while (subscriber.receivedEvents().isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }

        subscriber.receivedEvents().shouldHaveSize(1)
        subscriber.receivedEvents().first().payload shouldBe "redis-pubsub"
    }

    @Test
    @DisplayName("단순 락은 같은 키에 대해 하나의 토큰만 획득한다")
    fun `should only acquire one simple lock token`() {
        val first = simpleRedisLockService.acquire("lock:test", Duration.ofSeconds(3))
        val second = simpleRedisLockService.acquire("lock:test", Duration.ofSeconds(3))

        first.shouldNotBeNull()
        second shouldBe null
        simpleRedisLockService.release("lock:test", first) shouldBe true
    }

    @Test
    @DisplayName("Redisson 락으로 동시 요청에서도 재고가 음수가 되지 않는다")
    fun `should protect stock with redisson lock`() {
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

        latch.await(5, TimeUnit.SECONDS) shouldBe true
        executor.shutdownNow()

        redissonInventoryLockService.currentStock() shouldBe 0
    }

    @Test
    @DisplayName("Sorted Set으로 상위 랭킹과 개별 순위를 조회한다")
    fun `should manage leaderboard`() {
        leaderboardService.recordScore("alice", 120.0)
        leaderboardService.recordScore("bob", 99.0)
        leaderboardService.recordScore("charlie", 111.0)

        val top2 = leaderboardService.top(2)

        top2.map { it.member } shouldBe listOf("alice", "charlie")
        leaderboardService.rankOf("bob") shouldBe 2
    }

    @Test
    @DisplayName("고정 윈도우 rate limit은 최대 요청 수를 넘기면 차단한다")
    fun `should block requests over rate limit`() {
        val results = (1..4).map { rateLimitService.allow("client-a") }

        results.map { it.allowed } shouldBe listOf(true, true, true, false)
        stringRedisTemplate.getExpire("rate-limit:client-a").shouldBeLessThanOrEqual(2)
    }
}
