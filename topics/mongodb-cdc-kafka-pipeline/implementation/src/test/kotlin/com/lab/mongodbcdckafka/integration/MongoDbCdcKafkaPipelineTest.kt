package com.lab.mongodbcdckafka.integration

import com.lab.mongodbcdckafka.domain.CdcEvent
import com.lab.mongodbcdckafka.domain.CountSnapshot
import com.lab.mongodbcdckafka.domain.IncrementalProjection
import com.lab.mongodbcdckafka.domain.ModelingProfile
import com.lab.mongodbcdckafka.domain.OperationType
import com.lab.mongodbcdckafka.domain.PipelineStage
import com.lab.mongodbcdckafka.service.CdcVerificationEventRequest
import com.lab.mongodbcdckafka.service.CdcVerificationLabService
import com.lab.mongodbcdckafka.service.CdcVerificationRequest
import com.lab.mongodbcdckafka.service.FailureSimulationType
import com.lab.mongodbcdckafka.service.CdcVerificationService
import com.lab.mongodbcdckafka.service.KafkaFailureSimulationRequest
import com.lab.mongodbcdckafka.service.KafkaFailureSimulationService
import com.lab.mongodbcdckafka.service.KafkaDeliveryRiskService
import com.lab.mongodbcdckafka.service.ModelingDecisionRequest
import com.lab.mongodbcdckafka.service.MongoModelingDecisionService
import com.lab.mongodbcdckafka.service.MongoModelingLabService
import com.lab.mongodbcdckafka.service.OrderingGuaranteeService
import com.lab.mongodbcdckafka.service.ScopeFunctionGuideService
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
class MongoDbCdcKafkaPipelineTest {

    @Autowired
    private lateinit var cdcVerificationService: CdcVerificationService

    @Autowired
    private lateinit var kafkaDeliveryRiskService: KafkaDeliveryRiskService

    @Autowired
    private lateinit var orderingGuaranteeService: OrderingGuaranteeService

    @Autowired
    private lateinit var mongoModelingDecisionService: MongoModelingDecisionService

    @Autowired
    private lateinit var scopeFunctionGuideService: ScopeFunctionGuideService

    @Autowired
    private lateinit var cdcVerificationLabService: CdcVerificationLabService

    @Autowired
    private lateinit var mongoModelingLabService: MongoModelingLabService

    @Autowired
    private lateinit var kafkaFailureSimulationService: KafkaFailureSimulationService

    @Test
    @DisplayName("CDC 유실 검증 시 resume token 누락을 탐지한다")
    fun `should detect missing resume tokens`() {
        val events = listOf(
            event(resumeToken = 101, aggregateId = "order-1", version = 1),
            event(resumeToken = 102, aggregateId = "order-1", version = 2),
            event(resumeToken = 104, aggregateId = "order-2", version = 1),
        )

        val report = cdcVerificationService.detectGaps(events)

        report.missingResumeTokens shouldBe listOf(103L)
        report.findings.first() shouldContain "누락"
    }

    @Test
    @DisplayName("source topic sink 건수가 다르면 정합성 경고를 준다")
    fun `should reconcile counts across stages`() {
        val findings = cdcVerificationService.reconcileCounts(
            CountSnapshot(
                sourceCount = 120,
                topicCount = 119,
                sinkCount = 118,
            )
        )

        findings shouldHaveSize 2
    }

    @Test
    @DisplayName("aggregate 단위 순서가 유지되는지 검증한다")
    fun `should verify ordering within aggregate`() {
        val orderedEvents = listOf(
            event(resumeToken = 1, aggregateId = "order-1", version = 1),
            event(resumeToken = 2, aggregateId = "order-1", version = 2),
            event(resumeToken = 3, aggregateId = "order-2", version = 1),
            event(resumeToken = 4, aggregateId = "order-2", version = 2),
        )

        orderingGuaranteeService.isOrderedWithinAggregate(orderedEvents) shouldBe true
    }

    @Test
    @DisplayName("최신 projection 버전이 뒤처지면 경고를 준다")
    fun `should warn when projection lags behind latest version`() {
        val findings = cdcVerificationService.verifyLatestVersion(
            events = listOf(
                event(resumeToken = 1, aggregateId = "order-1", version = 1),
                event(resumeToken = 2, aggregateId = "order-1", version = 2),
            ),
            projections = listOf(
                IncrementalProjection(aggregateId = "order-1", latestAppliedVersion = 1),
            ),
        )

        findings.single() shouldContain "최신 버전 2"
    }

    @Test
    @DisplayName("Kafka 메시지 유실 가능 조건을 Producer Broker Consumer 축으로 설명한다")
    fun `should enumerate kafka delivery risks`() {
        val risks = kafkaDeliveryRiskService.knownRisks()

        risks.map { it.stage } shouldContain PipelineStage.PRODUCER
        risks.map { it.stage } shouldContain PipelineStage.BROKER
        risks.map { it.stage } shouldContain PipelineStage.CONSUMER
    }

    @Test
    @DisplayName("함께 읽고 함께 수정하는 작은 하위 구조는 Embed가 적합하다")
    fun `should choose embed for bounded aggregate data`() {
        val strategy = mongoModelingDecisionService.chooseStrategy(
            ModelingProfile(
                readTogether = true,
                updateTogether = true,
                boundedCardinality = true,
                independentlyQueriedChildren = false,
            )
        )

        strategy.name shouldBe "EMBED"
    }

    @Test
    @DisplayName("독립 조회가 필요한 큰 하위 구조는 Reference가 적합하다")
    fun `should choose reference for independently queried children`() {
        val strategy = mongoModelingDecisionService.chooseStrategy(
            ModelingProfile(
                readTogether = false,
                updateTogether = false,
                boundedCardinality = false,
                independentlyQueriedChildren = true,
            )
        )

        strategy.name shouldBe "REFERENCE"
    }

    @Test
    @DisplayName("Scope Function은 의도별 사용법을 설명할 수 있어야 한다")
    fun `should expose scope function guides`() {
        val guides = scopeFunctionGuideService.guides()

        guides shouldHaveSize 5
        guides.map { it.name } shouldContain "apply"
    }

    @Test
    @DisplayName("CDC 검증 API용 서비스는 누락 토큰과 count mismatch를 함께 돌려준다")
    fun `should build cdc verification response`() {
        val response = cdcVerificationLabService.verify(
            CdcVerificationRequest(
                events = listOf(
                    CdcVerificationEventRequest(resumeToken = 101, aggregateId = "order-1", version = 1),
                    CdcVerificationEventRequest(resumeToken = 103, aggregateId = "order-1", version = 2),
                ),
                countSnapshot = CountSnapshot(sourceCount = 2, topicCount = 2, sinkCount = 1),
                projections = listOf(
                    IncrementalProjection(aggregateId = "order-1", latestAppliedVersion = 1),
                ),
            )
        )

        response.missingResumeTokens shouldBe listOf(102L)
        response.countFindings.single() shouldContain "sink"
        response.versionFindings.single() shouldContain "최신 버전 2"
    }

    @Test
    @DisplayName("Mongo 모델링 판단 API용 서비스는 이유까지 함께 반환한다")
    fun `should explain modeling decision`() {
        val response = mongoModelingLabService.decide(
            ModelingDecisionRequest(
                readTogether = true,
                updateTogether = true,
                boundedCardinality = true,
                independentlyQueriedChildren = false,
            )
        )

        response.strategy.name shouldBe "EMBED"
        response.reason shouldContain "Embed"
    }

    @Test
    @DisplayName("Kafka 실패 시뮬레이션은 consumer 선커밋 시 sink mismatch를 보여준다")
    fun `should simulate commit before processing risk`() {
        val response = kafkaFailureSimulationService.simulate(
            KafkaFailureSimulationRequest(
                simulationType = FailureSimulationType.COMMIT_BEFORE_PROCESSING,
                sourceCount = 3,
                affectedEvents = 1,
            )
        )

        response.perceivedLoss shouldBe true
        response.offsetCommitted shouldBe true
        response.sinkUpdated shouldBe false
        response.countFindings.single() shouldContain "sink"
        response.matchedRisk?.stage shouldBe PipelineStage.CONSUMER
    }

    private fun event(
        resumeToken: Long,
        aggregateId: String,
        version: Long,
    ): CdcEvent<Map<String, String>> =
        CdcEvent(
            resumeToken = resumeToken,
            aggregateId = aggregateId,
            version = version,
            operation = OperationType.UPDATE,
            occurredAt = Instant.parse("2026-03-30T00:00:00Z"),
            payload = mapOf("status" to "UPDATED"),
        )
}
