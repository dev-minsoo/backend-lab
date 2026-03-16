package com.lab.redis.pubsub

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

@Component
class LabEventSubscriber(
    private val objectMapper: ObjectMapper,
) : MessageListener {
    private val messages = CopyOnWriteArrayList<LabEvent>()

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val event = objectMapper.readValue(message.body, LabEvent::class.java)
        messages += event
    }

    fun receivedEvents(): List<LabEvent> = messages.toList()

    fun clear() {
        messages.clear()
    }
}
