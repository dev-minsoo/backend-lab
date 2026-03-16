package com.lab.redis.pubsub

data class LabEvent(
    val type: String,
    val payload: String,
)
