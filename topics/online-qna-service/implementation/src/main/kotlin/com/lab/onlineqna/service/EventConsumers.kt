package com.lab.onlineqna.service

import com.lab.onlineqna.config.AppProperties
import com.lab.onlineqna.domain.Notification
import com.lab.onlineqna.event.ChangeType
import com.lab.onlineqna.event.NotificationCreatedEvent
import com.lab.onlineqna.event.QuestionChangedEvent
import com.lab.onlineqna.repository.NotificationRepository
import com.lab.onlineqna.repository.QuestionRepository
import com.lab.onlineqna.repository.UserRepository
import com.lab.onlineqna.support.toSearchDocument
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EventConsumers(
    private val questionRepository: QuestionRepository,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val searchIndexService: SearchIndexService
) {

    @EventListener
    @Transactional
    fun handleQuestionChanged(event: QuestionChangedEvent) {
        consumeQuestionChanged(event)
    }

    @EventListener
    @Transactional
    fun handleNotificationCreated(event: NotificationCreatedEvent) {
        consumeNotificationCreated(event)
    }

    @KafkaListener(
        topics = ["\${app.topics.question-changed}"],
        containerFactory = "kafkaListenerContainerFactory",
        autoStartup = "\${app.messaging.enabled:true}"
    )
    @Transactional
    fun onQuestionChanged(event: QuestionChangedEvent) {
        consumeQuestionChanged(event)
    }

    @KafkaListener(
        topics = ["\${app.topics.notification-created}"],
        containerFactory = "kafkaListenerContainerFactory",
        autoStartup = "\${app.messaging.enabled:true}"
    )
    @Transactional
    fun onNotificationCreated(event: NotificationCreatedEvent) {
        consumeNotificationCreated(event)
    }

    private fun consumeQuestionChanged(event: QuestionChangedEvent) {
        if (event.changeType == ChangeType.DELETE) {
            searchIndexService.delete(event.questionId)
            return
        }
        questionRepository.findActiveQuestionById(event.questionId)
            .ifPresent { searchIndexService.upsert(it.toSearchDocument()) }
    }

    private fun consumeNotificationCreated(event: NotificationCreatedEvent) {
        val user = userRepository.findById(event.userId).orElse(null) ?: return
        notificationRepository.save(
            Notification(
                user = user,
                message = event.message,
                referenceId = event.referenceId,
                referenceType = event.referenceType
            )
        )
    }
}
