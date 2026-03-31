package com.lab.mongodbcdckafka.config

import com.lab.mongodbcdckafka.domain.OrderCdcEvent
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaLabConfig {

    @Bean
    fun orderEventsTopic(properties: PipelineProperties): NewTopic =
        NewTopic(properties.topic, properties.partitionCount, 1)

    @Bean
    fun orderEventProducerFactory(kafkaProperties: KafkaProperties): ProducerFactory<String, OrderCdcEvent> {
        val config = HashMap<String, Any>(kafkaProperties.buildProducerProperties())
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun orderEventKafkaTemplate(producerFactory: ProducerFactory<String, OrderCdcEvent>): KafkaTemplate<String, OrderCdcEvent> =
        KafkaTemplate(producerFactory)

    @Bean
    fun orderEventConsumerFactory(kafkaProperties: KafkaProperties): ConsumerFactory<String, OrderCdcEvent> {
        val config = HashMap<String, Any>(kafkaProperties.buildConsumerProperties())
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        config[JsonDeserializer.TRUSTED_PACKAGES] = "com.lab.mongodbcdckafka.domain"
        config[JsonDeserializer.VALUE_DEFAULT_TYPE] = OrderCdcEvent::class.java.name
        config[JsonDeserializer.USE_TYPE_INFO_HEADERS] = false
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun orderEventKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, OrderCdcEvent>,
        properties: PipelineProperties,
    ): ConcurrentKafkaListenerContainerFactory<String, OrderCdcEvent> =
        ConcurrentKafkaListenerContainerFactory<String, OrderCdcEvent>().apply {
            this.consumerFactory = consumerFactory
            setConcurrency(properties.consumerConcurrency)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        }
}

