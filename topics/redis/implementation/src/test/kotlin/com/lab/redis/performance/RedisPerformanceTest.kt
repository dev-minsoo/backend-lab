package com.lab.redis.performance

import com.lab.redis.cache.ProductCatalogService
import com.lab.redis.integration.RedisIntegrationTestSupport
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.system.measureTimeMillis

class RedisPerformanceTest : RedisIntegrationTestSupport() {
    @Autowired
    private lateinit var productCatalogService: ProductCatalogService

    @Test
    @DisplayName("같은 키 반복 조회에서 캐시가 원본 조회 횟수를 줄인다")
    fun `performance comparison`() {
        productCatalogService.resetCounter()
        productCatalogService.evictProduct(1L)

        val coldTime = measureTimeMillis {
            productCatalogService.getProduct(1L)
        }

        val warmTime = measureTimeMillis {
            repeat(50) {
                productCatalogService.getProduct(1L)
            }
        }

        productCatalogService.queryCount() shouldBe 1
        (coldTime >= 0L).shouldBe(true)
        (warmTime >= 0L).shouldBe(true)
    }
}
