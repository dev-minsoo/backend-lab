package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.config.PipelineProperties
import com.lab.mongodbcdckafka.domain.OperationType
import com.lab.mongodbcdckafka.domain.OrderCdcEvent
import com.lab.mongodbcdckafka.domain.OrderStatusPayload
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

@Service
class OrderEventPublisherService(
    private val kafkaTemplate: KafkaTemplate<String, OrderCdcEvent>,
    private val properties: PipelineProperties,
) {
    private val resumeTokenSequence = AtomicLong(1_000)

    fun publish(request: PublishOrderEventRequest): OrderCdcEvent {
        val event = OrderCdcEvent(
            resumeToken = request.resumeToken ?: resumeTokenSequence.incrementAndGet(),
            aggregateId = request.aggregateId,
            version = request.version,
            operation = request.operation,
            occurredAt = request.occurredAt ?: Instant.now(),
            payload = OrderStatusPayload(
                status = request.status,
                customerId = request.customerId,
                totalAmount = request.totalAmount,
            ),
        )

        kafkaTemplate.send(properties.topic, event.aggregateId, event)
        return event
    }
}

data class PublishOrderEventRequest(
    val aggregateId: String,
    val version: Long,
    val status: String,
    val customerId: String,
    val totalAmount: Long,
    val operation: OperationType = OperationType.UPDATE,
    val resumeToken: Long? = null,
    val occurredAt: Instant? = null,
)

