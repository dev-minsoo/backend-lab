package com.lab.redis.demo

data class RedisDemoActionDetails(
    val title: String,
    val summary: String,
    val expected: String,
    val actual: String,
    val redisKey: String,
    val redisCommand: String,
)
