package com.lab.mongodbcdckafka.domain

data class KafkaDeliveryRisk(
    val stage: PipelineStage,
    val condition: String,
    val consequence: String,
    val mitigation: String,
)

enum class PipelineStage {
    PRODUCER,
    BROKER,
    CONSUMER,
}
