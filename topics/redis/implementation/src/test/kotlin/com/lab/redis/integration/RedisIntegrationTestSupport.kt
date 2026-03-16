package com.lab.redis.integration

import jakarta.annotation.PreDestroy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import redis.embedded.RedisServer
import java.net.ServerSocket

@SpringBootTest
abstract class RedisIntegrationTestSupport {
    companion object {
        private val redisPort = ServerSocket(0).use { it.localPort }
        private val redisServer = RedisServer(redisPort)

        init {
            redisServer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { "127.0.0.1" }
            registry.add("spring.data.redis.port") { redisPort }
        }
    }

    @BeforeEach
    @AfterEach
    fun flushRedis() {
        org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory("127.0.0.1", redisPort)
            .apply { afterPropertiesSet() }
            .connection
            .serverCommands()
            .flushAll()
    }

    @PreDestroy
    fun stopRedis() {
        redisServer.stop()
    }
}
