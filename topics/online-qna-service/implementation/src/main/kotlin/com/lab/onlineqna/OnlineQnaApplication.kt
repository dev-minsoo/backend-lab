package com.lab.onlineqna

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.cache.annotation.EnableCaching
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.boot.runApplication

@EnableAsync
@EnableCaching
@EnableKafka
@SpringBootApplication
@ConfigurationPropertiesScan
class OnlineQnaApplication

fun main(args: Array<String>) {
    runApplication<OnlineQnaApplication>(*args)
}
