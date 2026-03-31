package com.lab.mongodbcdckafka.consumer

import com.lab.mongodbcdckafka.domain.OrderCdcEvent
import com.lab.mongodbcdckafka.service.OrderProjectionService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderProjectionConsumer(
    private val orderProjectionService: OrderProjectionService,
) {

    @KafkaListener(
        topics = ["\${app.pipeline.topic}"],
        containerFactory = "orderEventKafkaListenerContainerFactory",
    )
    fun consume(event: OrderCdcEvent, acknowledgment: Acknowledgment) {
        orderProjectionService.apply(event)
        acknowledgment.acknowledge()
    }
}

