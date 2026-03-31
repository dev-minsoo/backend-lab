package com.lab.mongodbcdckafka.domain

import java.time.Instant

data class OrderCdcEvent(
    val resumeToken: Long,
    val aggregateId: String,
    val version: Long,
    val operation: OperationType,
    val occurredAt: Instant,
    val payload: OrderStatusPayload,
)

data class OrderStatusPayload(
    val status: String,
    val customerId: String,
    val totalAmount: Long,
)

