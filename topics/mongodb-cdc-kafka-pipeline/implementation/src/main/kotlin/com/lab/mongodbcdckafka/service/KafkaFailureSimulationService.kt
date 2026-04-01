package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.CountSnapshot
import com.lab.mongodbcdckafka.domain.KafkaDeliveryRisk
import com.lab.mongodbcdckafka.domain.PipelineStage
import org.springframework.stereotype.Service

@Service
class KafkaFailureSimulationService(
    private val cdcVerificationService: CdcVerificationService,
    private val kafkaDeliveryRiskService: KafkaDeliveryRiskService,
) {

    fun simulate(request: KafkaFailureSimulationRequest): KafkaFailureSimulationResponse {
        val scenario = when (request.simulationType) {
            FailureSimulationType.COMMIT_BEFORE_PROCESSING ->
                SimulationScenario(
                    stage = PipelineStage.CONSUMER,
                    offsetCommitted = true,
                    sinkUpdated = false,
                    dlqWritten = false,
                    summary = "offset을 먼저 commit하고 처리에 실패해 sink 반영이 비었습니다.",
                    matchingCondition = "실제 처리 전에 offset을 먼저 commit한 경우",
                )

            FailureSimulationType.SWALLOW_EXCEPTION_WITHOUT_DLQ ->
                SimulationScenario(
                    stage = PipelineStage.CONSUMER,
                    offsetCommitted = true,
                    sinkUpdated = false,
                    dlqWritten = false,
                    summary = "예외를 삼키고 DLQ를 남기지 않아 운영자가 실패를 놓치기 쉽습니다.",
                    matchingCondition = "예외를 삼키고 DLQ도 남기지 않은 경우",
                )

            FailureSimulationType.BROKER_REPLICATION_GAP ->
                SimulationScenario(
                    stage = PipelineStage.BROKER,
                    offsetCommitted = false,
                    sinkUpdated = false,
                    dlqWritten = false,
                    summary = "브로커 복제 안정성이 부족하면 topic 적재 자체가 비어 보일 수 있습니다.",
                    matchingCondition = "복제되지 않은 상태에서 leader 장애가 나거나 위험한 leader election 설정을 둔 경우",
                )

            FailureSimulationType.SAFE_RETRY_WITH_DLQ ->
                SimulationScenario(
                    stage = PipelineStage.CONSUMER,
                    offsetCommitted = false,
                    sinkUpdated = false,
                    dlqWritten = true,
                    summary = "처리 실패를 DLQ로 보내고 commit을 미뤄 유실처럼 보이지 않게 합니다.",
                    matchingCondition = "실제 처리 전에 offset을 먼저 commit한 경우",
                )
        }

        val snapshot = when (request.simulationType) {
            FailureSimulationType.BROKER_REPLICATION_GAP ->
                CountSnapshot(
                    sourceCount = request.sourceCount,
                    topicCount = request.sourceCount - request.affectedEvents,
                    sinkCount = request.sourceCount - request.affectedEvents,
                )

            FailureSimulationType.SAFE_RETRY_WITH_DLQ ->
                CountSnapshot(
                    sourceCount = request.sourceCount,
                    topicCount = request.sourceCount,
                    sinkCount = request.sourceCount,
                )

            else ->
                CountSnapshot(
                    sourceCount = request.sourceCount,
                    topicCount = request.sourceCount,
                    sinkCount = request.sourceCount - request.affectedEvents,
                )
        }

        val findings = cdcVerificationService.reconcileCounts(snapshot)
        val matchedRisk = kafkaDeliveryRiskService.knownRisks().firstOrNull {
            it.stage == scenario.stage && it.condition == scenario.matchingCondition
        }

        return KafkaFailureSimulationResponse(
            simulationType = request.simulationType,
            snapshot = snapshot,
            offsetCommitted = scenario.offsetCommitted,
            sinkUpdated = scenario.sinkUpdated,
            dlqWritten = scenario.dlqWritten,
            perceivedLoss = snapshot.topicCount != snapshot.sinkCount || snapshot.sourceCount != snapshot.topicCount,
            summary = scenario.summary,
            countFindings = findings,
            matchedRisk = matchedRisk,
        )
    }
}

data class KafkaFailureSimulationRequest(
    val simulationType: FailureSimulationType,
    val sourceCount: Long = 1,
    val affectedEvents: Long = 1,
)

enum class FailureSimulationType {
    COMMIT_BEFORE_PROCESSING,
    SWALLOW_EXCEPTION_WITHOUT_DLQ,
    BROKER_REPLICATION_GAP,
    SAFE_RETRY_WITH_DLQ,
}

data class KafkaFailureSimulationResponse(
    val simulationType: FailureSimulationType,
    val snapshot: CountSnapshot,
    val offsetCommitted: Boolean,
    val sinkUpdated: Boolean,
    val dlqWritten: Boolean,
    val perceivedLoss: Boolean,
    val summary: String,
    val countFindings: List<String>,
    val matchedRisk: KafkaDeliveryRisk?,
)

private data class SimulationScenario(
    val stage: PipelineStage,
    val offsetCommitted: Boolean,
    val sinkUpdated: Boolean,
    val dlqWritten: Boolean,
    val summary: String,
    val matchingCondition: String,
)

