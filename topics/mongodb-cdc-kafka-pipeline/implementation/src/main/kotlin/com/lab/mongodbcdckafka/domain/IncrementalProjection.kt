package com.lab.mongodbcdckafka.domain

data class IncrementalProjection(
    val aggregateId: String,
    val latestAppliedVersion: Long,
)

data class CountSnapshot(
    val sourceCount: Long,
    val topicCount: Long,
    val sinkCount: Long,
)

data class GapReport(
    val missingResumeTokens: List<Long>,
    val findings: List<String>,
)
