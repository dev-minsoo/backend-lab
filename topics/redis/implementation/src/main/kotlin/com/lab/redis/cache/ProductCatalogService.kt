package com.lab.redis.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.io.Serializable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class ProductView(
    val id: Long,
    val name: String,
    val price: Int,
    val loadedAtEpochMillis: Long,
) : Serializable

@Service
class ProductCatalogService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val queryCounter = AtomicInteger(0)
    private val storage = ConcurrentHashMap<Long, Pair<String, Int>>(
        mapOf(
            1L to ("keyboard" to 79000),
            2L to ("mouse" to 39000),
        ),
    )

    fun getProduct(productId: Long): ProductView {
        val cacheKey = cacheKey(productId)
        val cached = stringRedisTemplate.opsForValue().get(cacheKey)?.let {
            objectMapper.readValue(it, ProductView::class.java)
        }
        if (cached != null) {
            logger.info("demo=cache source=redis key={}", cacheKey)
            return cached
        }

        queryCounter.incrementAndGet()
        val product = storage[productId] ?: error("product not found: $productId")
        val productView = ProductView(
            id = productId,
            name = product.first,
            price = product.second,
            loadedAtEpochMillis = System.currentTimeMillis(),
        )
        stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(productView), Duration.ofMinutes(10))
        logger.info("demo=cache source=origin key={} queryCount={}", cacheKey, queryCounter.get())
        return productView
    }

    fun updatePrice(productId: Long, newPrice: Int): ProductView {
        val current = storage[productId] ?: error("product not found: $productId")
        storage[productId] = current.first to newPrice
        val updated = getProductWithoutCache(productId)
        stringRedisTemplate.opsForValue().set(cacheKey(productId), objectMapper.writeValueAsString(updated), Duration.ofMinutes(10))
        return updated
    }

    fun evictProduct(productId: Long) {
        stringRedisTemplate.delete(cacheKey(productId))
    }

    fun resetCounter() {
        queryCounter.set(0)
    }

    fun queryCount(): Int = queryCounter.get()

    fun cachedProduct(productId: Long): ProductView? =
        stringRedisTemplate.opsForValue().get(cacheKey(productId))?.let {
            objectMapper.readValue(it, ProductView::class.java)
        }

    private fun getProductWithoutCache(productId: Long): ProductView {
        queryCounter.incrementAndGet()
        val product = storage[productId] ?: error("product not found: $productId")
        return ProductView(
            id = productId,
            name = product.first,
            price = product.second,
            loadedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    private fun cacheKey(productId: Long): String = "products::$productId"
}
