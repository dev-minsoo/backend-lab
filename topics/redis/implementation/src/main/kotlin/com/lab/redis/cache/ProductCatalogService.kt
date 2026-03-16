package com.lab.redis.cache

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class ProductView(
    val id: Long,
    val name: String,
    val price: Int,
    val loadedAtEpochMillis: Long,
) : Serializable

@Service
class ProductCatalogService {
    private val queryCounter = AtomicInteger(0)
    private val storage = ConcurrentHashMap<Long, Pair<String, Int>>(
        mapOf(
            1L to ("keyboard" to 79000),
            2L to ("mouse" to 39000),
        ),
    )

    @Cacheable(cacheNames = ["products"], key = "#productId")
    fun getProduct(productId: Long): ProductView {
        queryCounter.incrementAndGet()
        val product = storage[productId] ?: error("product not found: $productId")
        return ProductView(
            id = productId,
            name = product.first,
            price = product.second,
            loadedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    @CachePut(cacheNames = ["products"], key = "#productId")
    fun updatePrice(productId: Long, newPrice: Int): ProductView {
        val current = storage[productId] ?: error("product not found: $productId")
        storage[productId] = current.first to newPrice
        return getProductWithoutCache(productId)
    }

    @CacheEvict(cacheNames = ["products"], key = "#productId")
    fun evictProduct(productId: Long) {
        // Intentionally empty. Annotation performs the eviction.
    }

    fun resetCounter() {
        queryCounter.set(0)
    }

    fun queryCount(): Int = queryCounter.get()

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
}
