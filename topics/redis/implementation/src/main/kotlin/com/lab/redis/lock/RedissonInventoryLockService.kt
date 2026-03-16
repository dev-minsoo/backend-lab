package com.lab.redis.lock

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Service
class RedissonInventoryLockService(
    private val redissonClient: RedissonClient,
) {
    private val stock = AtomicInteger(5)

    fun resetStock(quantity: Int) {
        stock.set(quantity)
    }

    fun currentStock(): Int = stock.get()

    fun decreaseWithLock(): Boolean {
        val lock = redissonClient.getLock("lock:inventory")
        val acquired = lock.tryLock(3, 3, TimeUnit.SECONDS)
        if (!acquired) {
            return false
        }

        return try {
            if (stock.get() <= 0) {
                false
            } else {
                stock.decrementAndGet()
                true
            }
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
