package com.lab.redis.demo

import com.lab.redis.basics.RedisBasicsService
import com.lab.redis.leaderboard.LeaderboardService
import com.lab.redis.lock.RedissonInventoryLockService
import com.lab.redis.lock.SimpleRedisLockService
import com.lab.redis.pubsub.LabEvent
import com.lab.redis.pubsub.LabEventPublisher
import com.lab.redis.pubsub.LabEventSubscriber
import com.lab.redis.ratelimit.RateLimitService
import com.lab.redis.cache.ProductCatalogService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class RedisDemoActionService(
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
    private val activityLogService: RedisDemoActivityLogService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun snapshot(): RedisDemoSnapshot = snapshotService.snapshot()

    fun populateBasics(): RedisDemoResponse {
        val snapshot = redisBasicsService.populateAndRead()
        val message = "기본 자료구조 데이터를 채웠습니다. TTL=${snapshot.ttlSeconds}s, Top=${snapshot.sortedTop.joinToString()}"
        logger.info("demo=basics action=populate ttl={} top={}", snapshot.ttlSeconds, snapshot.sortedTop)
        val details = RedisDemoActionDetails(
            title = "Basics",
            summary = "Redis는 문자열만 저장하는 것이 아니라 여러 자료구조를 직접 제공한다는 점을 보여줍니다.",
            expected = "String, Hash, Set, List, Sorted Set 값이 저장되고 TTL이 0보다 큰 값으로 보입니다.",
            actual = "string=${snapshot.stringValue}, hash=${snapshot.hashValue}, ttl=${snapshot.ttlSeconds}s, zsetTop=${snapshot.sortedTop.joinToString()}",
            redisKey = "basics:string, basics:hash, basics:set, basics:list, basics:zset",
            redisCommand = "SET, HSET, SADD, RPUSH, ZADD, EXPIRE",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response(message, details)
    }

    fun runCacheDemo(): RedisDemoResponse {
        productCatalogService.resetCounter()
        val first = productCatalogService.getProduct(1L)
        val second = productCatalogService.getProduct(1L)
        val message = "캐시 데모 완료. firstLoadedAt=${first.loadedAtEpochMillis}, secondLoadedAt=${second.loadedAtEpochMillis}, queryCount=${productCatalogService.queryCount()}"
        logger.info("demo=cache action=run queryCount={} cachedProductId={}", productCatalogService.queryCount(), first.id)
        val details = RedisDemoActionDetails(
            title = "Cache",
            summary = "같은 데이터를 두 번 요청해도 원본 조회는 한 번만 수행되고 두 번째부터는 Redis 캐시를 사용합니다.",
            expected = "두 번의 메서드 호출 후 queryCount가 1이고, Redis에 products::1 키가 JSON으로 저장됩니다.",
            actual = "productId=${first.id}, queryCount=${productCatalogService.queryCount()}, firstLoadedAt=${first.loadedAtEpochMillis}, secondLoadedAt=${second.loadedAtEpochMillis}",
            redisKey = "products::1",
            redisCommand = "GET, SETEX",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response(message, details)
    }

    fun evictCache(): RedisDemoResponse {
        productCatalogService.evictProduct(1L)
        logger.info("demo=cache action=evict key=products::1")
        val details = RedisDemoActionDetails(
            title = "Cache Evict",
            summary = "캐시 무효화로 Redis에 저장된 값을 지웁니다.",
            expected = "products::1 키가 삭제되어 다음 조회 시 원본 조회가 다시 발생합니다.",
            actual = "products::1 키 삭제 요청 수행",
            redisKey = "products::1",
            redisCommand = "DEL",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response("products 캐시에서 product:1 을 제거했습니다.", details)
    }

    fun publishPubSubEvent(): RedisDemoResponse {
        val payload = "study-session-${LocalDateTime.now()}"
        publisher.publish(LabEvent(type = "study", payload = payload))
        val deadline = System.currentTimeMillis() + 1_000
        while (subscriber.receivedEvents().none { it.payload == payload } && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        logger.info("demo=pubsub action=publish payload={} receivedCount={}", payload, subscriber.receivedEvents().size)
        val details = RedisDemoActionDetails(
            title = "Pub/Sub",
            summary = "발행 시점에 구독 중인 소비자만 이벤트를 받는 Redis Pub/Sub 특성을 보여줍니다.",
            expected = "이벤트 발행 직후 수신 목록에 같은 payload가 추가됩니다. 저장된 backlog는 없습니다.",
            actual = "publishedPayload=$payload, receivedCount=${subscriber.receivedEvents().size}",
            redisKey = "channel: lab:events",
            redisCommand = "PUBLISH, SUBSCRIBE",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response("Pub/Sub 이벤트를 발행했습니다: $payload", details)
    }

    fun simpleLockDemo(): RedisDemoResponse {
        val first = simpleRedisLockService.acquire("lock:demo", Duration.ofSeconds(5))
        val second = simpleRedisLockService.acquire("lock:demo", Duration.ofSeconds(5))
        val released = if (first != null) simpleRedisLockService.release("lock:demo", first) else false
        logger.info("demo=lock action=simple first={} second={} released={}", first != null, second != null, released)
        val details = RedisDemoActionDetails(
            title = "Simple Lock",
            summary = "같은 락 키에 대해 첫 번째 요청만 락을 얻고 두 번째 요청은 실패하는 기본 락 동작을 보여줍니다.",
            expected = "first=true, second=false, released=true 가 되어 같은 락을 동시에 둘이 갖지 못합니다.",
            actual = "first=${first != null}, second=${second != null}, released=$released",
            redisKey = "lock:demo",
            redisCommand = "SET key value NX PX, GET, DEL",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response("단순 락 결과: first=${first != null}, second=${second != null}, released=$released", details)
    }

    fun redissonLockDemo(): RedisDemoResponse {
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
        logger.info("demo=lock action=redisson finalStock={}", redissonInventoryLockService.currentStock())
        val details = RedisDemoActionDetails(
            title = "Redisson Lock",
            summary = "재고 5개에 대해 20개 동시 요청이 들어와도 락으로 임계 구역을 보호해 음수 재고가 생기지 않게 합니다.",
            expected = "20개 요청이 와도 최종 재고는 0이고 음수로 내려가지 않습니다.",
            actual = "initialStock=5, concurrentRequests=20, finalStock=${redissonInventoryLockService.currentStock()}",
            redisKey = "lock:inventory",
            redisCommand = "RLock.tryLock / unlock",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response("Redisson 락 데모 완료. 20개 요청 후 stock=${redissonInventoryLockService.currentStock()}", details)
    }

    fun seedLeaderboard(): RedisDemoResponse {
        leaderboardService.recordScore("alice", 120.0)
        leaderboardService.recordScore("charlie", 111.0)
        leaderboardService.recordScore("bob", 99.0)
        logger.info("demo=leaderboard action=seed top={}", leaderboardService.top(3))
        val details = RedisDemoActionDetails(
            title = "Leaderboard",
            summary = "Sorted Set은 점수(score)를 기준으로 자동 정렬되기 때문에 랭킹 기능에 적합합니다.",
            expected = "alice(120), charlie(111), bob(99) 순으로 자동 정렬됩니다.",
            actual = "top=${leaderboardService.top(3)}",
            redisKey = "leaderboard:study",
            redisCommand = "ZADD, ZREVRANGE, ZREVRANK",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response("리더보드 점수를 적재했습니다.", details)
    }

    fun hitRateLimit(): RedisDemoResponse {
        val result = rateLimitService.allow("demo-user")
        logger.info("demo=ratelimit action=hit allowed={} current={}", result.allowed, result.currentCount)
        val details = RedisDemoActionDetails(
            title = "Rate Limit",
            summary = "짧은 시간 동안 요청 횟수를 세고 제한하는 기능을 Redis 카운터로 구현합니다.",
            expected = "5초 동안 3번까지만 허용되고, 4번째부터는 차단됩니다.",
            actual = "allowed=${result.allowed}, currentCount=${result.currentCount}",
            redisKey = "rate-limit:demo-user",
            redisCommand = "INCR, EXPIRE",
        )
        activityLogService.append(details.title, details.actual, details.redisKey, details.redisCommand)
        return response("Rate Limit 결과: allowed=${result.allowed}, current=${result.currentCount}", details)
    }

    fun reset(): RedisDemoResponse {
        stringRedisTemplate.connectionFactory!!.connection.serverCommands().flushAll()
        subscriber.clear()
        productCatalogService.resetCounter()
        redissonInventoryLockService.resetStock(5)
        activityLogService.clear()
        logger.info("demo=all action=reset")
        val details = RedisDemoActionDetails(
            title = "Reset",
            summary = "데모 상태를 초기화해 다시 처음부터 시연할 수 있게 만듭니다.",
            expected = "Redis 키와 화면 상태가 초기값으로 돌아갑니다.",
            actual = "Redis FLUSHALL 수행, 로컬 로그 초기화",
            redisKey = "*",
            redisCommand = "FLUSHALL",
        )
        return response("Redis 상태와 데모 상태를 초기화했습니다.", details)
    }

    private fun response(message: String, details: RedisDemoActionDetails): RedisDemoResponse =
        RedisDemoResponse(
            message = message,
            details = details,
            snapshot = snapshotService.snapshot(),
        )
}
