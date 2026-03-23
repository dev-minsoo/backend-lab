package com.lab.onlineqna.service

import com.lab.onlineqna.config.AppProperties
import com.lab.onlineqna.event.NotificationCreatedEvent
import com.lab.onlineqna.event.QuestionChangedEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.messaging.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val appProperties: AppProperties
) : EventPublisher {

    override fun publishQuestionChanged(event: QuestionChangedEvent) {
        kafkaTemplate.send(appProperties.topics.questionChanged, event.questionId.toString(), event)
    }

    override fun publishNotificationCreated(event: NotificationCreatedEvent) {
        kafkaTemplate.send(appProperties.topics.notificationCreated, event.userId.toString(), event)
    }
}
