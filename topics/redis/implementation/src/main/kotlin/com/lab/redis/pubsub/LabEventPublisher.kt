package com.lab.redis.pubsub

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.stereotype.Service

@Service
class LabEventPublisher(
    private val stringRedisTemplate: StringRedisTemplate,
    private val topic: ChannelTopic,
    private val objectMapper: ObjectMapper,
) {
    fun publish(event: LabEvent) {
        stringRedisTemplate.convertAndSend(topic.topic, objectMapper.writeValueAsString(event))
    }
}
