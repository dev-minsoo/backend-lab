package com.lab.mongodbcdckafka.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "order_projection")
data class OrderProjectionDocument(
    @Id
    val aggregateId: String,
    val latestAppliedVersion: Long,
    val status: String,
    val customerId: String,
    val totalAmount: Long,
    val lastEventAt: Instant,
)

