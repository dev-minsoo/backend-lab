package com.lab.redis.integration

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class RedisDemoPageTest : RedisIntegrationTestSupport() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("Redis 데모 페이지가 렌더링된다")
    fun `should render redis demo page`() {
        mockMvc.perform(get("/demo/redis"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Redis 종합편 v1 데모")))
    }
}
