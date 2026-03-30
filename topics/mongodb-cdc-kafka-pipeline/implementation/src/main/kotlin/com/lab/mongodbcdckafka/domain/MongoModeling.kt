package com.lab.mongodbcdckafka.domain

data class ModelingProfile(
    val readTogether: Boolean,
    val updateTogether: Boolean,
    val boundedCardinality: Boolean,
    val independentlyQueriedChildren: Boolean,
)

enum class MongoModelingStrategy {
    EMBED,
    REFERENCE,
}

data class OrderLine(
    val lineId: String,
    val productId: String,
    val quantity: Int,
)

data class EmbeddedOrderDocument(
    val orderId: String,
    val customerId: String,
    val lines: List<OrderLine>,
)

data class ReferencedOrderDocument(
    val orderId: String,
    val customerId: String,
    val lineIds: List<String>,
)
