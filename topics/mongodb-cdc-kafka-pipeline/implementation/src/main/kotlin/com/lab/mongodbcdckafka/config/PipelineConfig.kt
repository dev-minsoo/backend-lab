package com.lab.mongodbcdckafka.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PipelineProperties::class)
class PipelineConfig
