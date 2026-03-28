package com.lab.onlineqna.config

import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

@Configuration
class DataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.write")
    fun writeDataSourceProperties() = DataSourceProperties()

    @Bean
    @ConfigurationProperties("app.datasource.read")
    fun readDataSourceProperties() = DataSourceProperties()

    @Bean
    fun writeDataSource(
        @Qualifier("writeDataSourceProperties") properties: DataSourceProperties
    ): DataSource = properties.initializeDataSourceBuilder()
        .type(HikariDataSource::class.java)
        .build()

    @Bean
    fun readDataSource(
        @Qualifier("readDataSourceProperties") properties: DataSourceProperties
    ): DataSource = properties.initializeDataSourceBuilder()
        .type(HikariDataSource::class.java)
        .build()

    @Bean
    fun routingDataSource(
        @Qualifier("writeDataSource") writeDataSource: DataSource,
        @Qualifier("readDataSource") readDataSource: DataSource
    ): DataSource {
        val routingDataSource = object : AbstractRoutingDataSource() {
            override fun determineCurrentLookupKey(): Any = DataSourceRoutingDecider.determine(
                isReadOnlyTransaction = TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
                isForceWrite = DataSourceRoutingContextHolder.isForceWrite()
            )
        }
        routingDataSource.setDefaultTargetDataSource(writeDataSource)
        routingDataSource.setTargetDataSources(
            mapOf(
                DataSourceType.WRITE to writeDataSource,
                DataSourceType.READ to readDataSource
            )
        )
        routingDataSource.afterPropertiesSet()
        return routingDataSource
    }

    @Bean
    @Primary
    fun dataSource(@Qualifier("routingDataSource") routingDataSource: DataSource): DataSource =
        LazyConnectionDataSourceProxy(routingDataSource)
}

enum class DataSourceType {
    WRITE,
    READ
}
