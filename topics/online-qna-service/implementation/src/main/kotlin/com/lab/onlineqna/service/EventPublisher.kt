package com.lab.onlineqna.service

import com.lab.onlineqna.event.NotificationCreatedEvent
import com.lab.onlineqna.event.QuestionChangedEvent

interface EventPublisher {
    fun publishQuestionChanged(event: QuestionChangedEvent)
    fun publishNotificationCreated(event: NotificationCreatedEvent)
}
