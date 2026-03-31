package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.OrderCdcEvent
import com.lab.mongodbcdckafka.domain.OrderProjectionDocument
import com.lab.mongodbcdckafka.repository.OrderProjectionRepository
import org.springframework.stereotype.Service

@Service
class OrderProjectionService(
    private val orderProjectionRepository: OrderProjectionRepository,
) {

    fun apply(event: OrderCdcEvent): OrderProjectionDocument {
        val existing = orderProjectionRepository.findById(event.aggregateId).orElse(null)

        if (existing != null && existing.latestAppliedVersion >= event.version) {
            return existing
        }

        val next = OrderProjectionDocument(
            aggregateId = event.aggregateId,
            latestAppliedVersion = event.version,
            status = event.payload.status,
            customerId = event.payload.customerId,
            totalAmount = event.payload.totalAmount,
            lastEventAt = event.occurredAt,
        )

        return orderProjectionRepository.save(next)
    }

    fun findProjection(aggregateId: String): OrderProjectionDocument? =
        orderProjectionRepository.findById(aggregateId).orElse(null)

    fun findAll(): List<OrderProjectionDocument> = orderProjectionRepository.findAll()
}

