package com.lab.mongodbcdckafka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MongoDbCdcKafkaPipelineApplication

fun main(args: Array<String>) {
    runApplication<MongoDbCdcKafkaPipelineApplication>(*args)
}
