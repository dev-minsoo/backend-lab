package com.lab.onlineqna.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory

@Configuration
class KafkaConfig {

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
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        objectMapper: ObjectMapper
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory
        factory.setRecordMessageConverter(
            org.springframework.kafka.support.converter.StringJsonMessageConverter(objectMapper)
        )
        return factory
    }
}
