package com.lab.mongodbcdckafka.domain

import java.time.Instant

data class CdcEvent<T>(
    val resumeToken: Long,
    val aggregateId: String,
    val version: Long,
    val operation: OperationType,
    val occurredAt: Instant,
    val payload: T,
)

enum class OperationType {
    INSERT,
    UPDATE,
    DELETE,
}
