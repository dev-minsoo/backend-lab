package com.lab.redis.demo

data class RedisDemoResponse(
    val message: String,
    val details: RedisDemoActionDetails,
    val snapshot: RedisDemoSnapshot,
)
