package com.lab.mongodbcdckafka.performance

import com.lab.mongodbcdckafka.domain.CdcEvent
import com.lab.mongodbcdckafka.domain.ModelingProfile
import com.lab.mongodbcdckafka.domain.OperationType
import com.lab.mongodbcdckafka.service.MongoModelingDecisionService
import com.lab.mongodbcdckafka.service.OrderingGuaranteeService
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.system.measureTimeMillis

@SpringBootTest
@ActiveProfiles("test")
class MongoDbCdcKafkaPipelinePerformanceTest {

    @Autowired
    private lateinit var orderingGuaranteeService: OrderingGuaranteeService

    @Autowired
    private lateinit var mongoModelingDecisionService: MongoModelingDecisionService

    companion object {
        private const val DATA_SIZE = 10_000
    }

    @Test
    @DisplayName("aggregate key 기준 partition 계산 비용을 확인한다")
    fun `partition assignment performance comparison`() {
        val events = (1..DATA_SIZE).map { index ->
            CdcEvent(
                resumeToken = index.toLong(),
                aggregateId = "order-${index % 250}",
                version = (index / 250 + 1).toLong(),
                operation = OperationType.UPDATE,
                occurredAt = Instant.parse("2026-03-30T00:00:00Z"),
                payload = mapOf("sequence" to index),
            )
        }

        val elapsed = measureTimeMillis {
            orderingGuaranteeService.partitionAssignments(events, partitionCount = 3)
        }

        println("10,000건 partition assignment 계산 시간: ${elapsed}ms")
        (elapsed >= 0L) shouldBe true
    }

    @Test
    @DisplayName("Embed와 Reference 선택 기준을 대량 프로필에 적용해도 일관된 결과를 낸다")
    fun `modeling decision consistency`() {
        val embeddedProfiles = List(DATA_SIZE / 2) {
            ModelingProfile(
                readTogether = true,
                updateTogether = true,
                boundedCardinality = true,
                independentlyQueriedChildren = false,
            )
        }

        val referencedProfiles = List(DATA_SIZE / 2) {
            ModelingProfile(
                readTogether = false,
                updateTogether = false,
                boundedCardinality = false,
                independentlyQueriedChildren = true,
            )
        }

        val embeddedElapsed = measureTimeMillis {
            embeddedProfiles.forEach { profile ->
                mongoModelingDecisionService.chooseStrategy(profile).name shouldBe "EMBED"
            }
        }

        val referencedElapsed = measureTimeMillis {
            referencedProfiles.forEach { profile ->
                mongoModelingDecisionService.chooseStrategy(profile).name shouldBe "REFERENCE"
            }
        }

        println("Embed 의사결정 시간: ${embeddedElapsed}ms")
        println("Reference 의사결정 시간: ${referencedElapsed}ms")
    }
}
