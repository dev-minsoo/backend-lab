package com.lab.redis.demo

data class RedisDemoActivity(
    val at: String,
    val title: String,
    val summary: String,
    val redisKey: String,
    val redisCommand: String,
)
