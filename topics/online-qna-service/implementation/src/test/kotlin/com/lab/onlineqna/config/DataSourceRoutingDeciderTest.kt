package com.lab.onlineqna.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DataSourceRoutingDeciderTest {

    @Test
    @DisplayName("읽기 전용 트랜잭션은 replica로 라우팅한다")
    fun `should route read only transaction to read replica`() {
        DataSourceRoutingDecider.determine(
            isReadOnlyTransaction = true,
            isForceWrite = false
        ) shouldBe DataSourceType.READ
    }

    @Test
    @DisplayName("강제 primary 컨텍스트가 있으면 읽기 전용이어도 primary로 라우팅한다")
    fun `should route force write context to primary even for read only transaction`() {
        DataSourceRoutingDecider.determine(
            isReadOnlyTransaction = true,
            isForceWrite = true
        ) shouldBe DataSourceType.WRITE
    }

    @Test
    @DisplayName("쓰기 트랜잭션은 primary로 라우팅한다")
    fun `should route write transaction to primary`() {
        DataSourceRoutingDecider.determine(
            isReadOnlyTransaction = false,
            isForceWrite = false
        ) shouldBe DataSourceType.WRITE
    }
}
