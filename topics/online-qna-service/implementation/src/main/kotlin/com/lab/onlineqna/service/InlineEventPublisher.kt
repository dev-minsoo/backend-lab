package com.lab.onlineqna.service

import com.lab.onlineqna.event.NotificationCreatedEvent
import com.lab.onlineqna.event.QuestionChangedEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.messaging.enabled"], havingValue = "false")
class InlineEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : EventPublisher {

    override fun publishQuestionChanged(event: QuestionChangedEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    override fun publishNotificationCreated(event: NotificationCreatedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
