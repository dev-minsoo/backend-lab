package com.lab.onlineqna.config

import com.lab.onlineqna.event.NotificationCreatedEvent
import com.lab.onlineqna.event.QuestionChangedEvent
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
class KafkaConfig(
    private val kafkaProperties: KafkaProperties
) {

    @Bean
    fun questionChangedTopic(appProperties: AppProperties): NewTopic =
        TopicBuilder.name(appProperties.topics.questionChanged)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun notificationCreatedTopic(appProperties: AppProperties): NewTopic =
        TopicBuilder.name(appProperties.topics.notificationCreated)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun questionChangedConsumerFactory(): ConsumerFactory<String, QuestionChangedEvent> =
        DefaultKafkaConsumerFactory(
            kafkaProperties.buildConsumerProperties(null),
            StringDeserializer(),
            ErrorHandlingDeserializer(JsonDeserializer(QuestionChangedEvent::class.java, false))
        )

    @Bean
    fun notificationCreatedConsumerFactory(): ConsumerFactory<String, NotificationCreatedEvent> =
        DefaultKafkaConsumerFactory(
            kafkaProperties.buildConsumerProperties(null),
            StringDeserializer(),
            ErrorHandlingDeserializer(JsonDeserializer(NotificationCreatedEvent::class.java, false))
        )

    @Bean
    fun questionChangedKafkaListenerContainerFactory(
        questionChangedConsumerFactory: ConsumerFactory<String, QuestionChangedEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, QuestionChangedEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, QuestionChangedEvent>()
        factory.consumerFactory = questionChangedConsumerFactory
        factory.setCommonErrorHandler(DefaultErrorHandler())
        return factory
    }

    @Bean
    fun notificationCreatedKafkaListenerContainerFactory(
        notificationCreatedConsumerFactory: ConsumerFactory<String, NotificationCreatedEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, NotificationCreatedEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, NotificationCreatedEvent>()
        factory.consumerFactory = notificationCreatedConsumerFactory
        factory.setCommonErrorHandler(DefaultErrorHandler())
        return factory
    }

    @Bean
    fun kafkaListenerContainerFactory(
        questionChangedConsumerFactory: ConsumerFactory<String, QuestionChangedEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, QuestionChangedEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, QuestionChangedEvent>()
        factory.consumerFactory = questionChangedConsumerFactory
        factory.setCommonErrorHandler(DefaultErrorHandler())
        return factory
    }
}
