package com.lab.onlineqna.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: JwtProperties,
    val topics: TopicProperties,
    val messaging: FeatureToggleProperties,
    val search: FeatureToggleProperties
)

data class JwtProperties(
    val secret: String,
    val issuer: String,
    val accessTokenExpirationMinutes: Long
)

data class TopicProperties(
    val questionChanged: String,
    val notificationCreated: String
)

data class FeatureToggleProperties(
    val enabled: Boolean
)
