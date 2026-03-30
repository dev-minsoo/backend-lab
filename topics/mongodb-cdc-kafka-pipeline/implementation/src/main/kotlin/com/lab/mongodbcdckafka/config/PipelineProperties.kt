package com.lab.mongodbcdckafka.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.pipeline")
data class PipelineProperties(
    val topic: String = "order-events",
    val partitionCount: Int = 3,
    val consumerConcurrency: Int = 3,
)
