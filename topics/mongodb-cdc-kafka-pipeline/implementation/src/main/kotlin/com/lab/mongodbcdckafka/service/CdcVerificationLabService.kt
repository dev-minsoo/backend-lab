package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.CdcEvent
import com.lab.mongodbcdckafka.domain.CountSnapshot
import com.lab.mongodbcdckafka.domain.IncrementalProjection
import com.lab.mongodbcdckafka.domain.OperationType
import com.lab.mongodbcdckafka.domain.OrderStatusPayload
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CdcVerificationLabService(
    private val cdcVerificationService: CdcVerificationService,
) {

    fun verify(request: CdcVerificationRequest): CdcVerificationResponse {
        val events = request.events.map { event ->
            CdcEvent(
                resumeToken = event.resumeToken,
                aggregateId = event.aggregateId,
                version = event.version,
                operation = event.operation,
                occurredAt = event.occurredAt,
                payload = OrderStatusPayload(
                    status = event.status,
                    customerId = event.customerId,
                    totalAmount = event.totalAmount,
                ),
            )
        }

        val gapReport = cdcVerificationService.detectGaps(events)
        val countFindings = request.countSnapshot?.let(cdcVerificationService::reconcileCounts).orEmpty()
        val versionFindings = if (request.projections.isEmpty()) {
            emptyList()
        } else {
            cdcVerificationService.verifyLatestVersion(events, request.projections)
        }

        return CdcVerificationResponse(
            missingResumeTokens = gapReport.missingResumeTokens,
            gapFindings = gapReport.findings,
            countFindings = countFindings,
            versionFindings = versionFindings,
        )
    }
}

data class CdcVerificationRequest(
    val events: List<CdcVerificationEventRequest>,
    val countSnapshot: CountSnapshot? = null,
    val projections: List<IncrementalProjection> = emptyList(),
)

data class CdcVerificationEventRequest(
    val resumeToken: Long,
    val aggregateId: String,
    val version: Long,
    val operation: OperationType = OperationType.UPDATE,
    val occurredAt: Instant = Instant.now(),
    val status: String = "UPDATED",
    val customerId: String = "user-unknown",
    val totalAmount: Long = 0,
)

data class CdcVerificationResponse(
    val missingResumeTokens: List<Long>,
    val gapFindings: List<String>,
    val countFindings: List<String>,
    val versionFindings: List<String>,
)

