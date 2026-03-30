package com.lab.mongodbcdckafka.producer

import org.springframework.stereotype.Component

@Component
class PartitionKeyResolver {

    fun resolveKey(aggregateId: String): String = aggregateId

    fun assignPartition(aggregateId: String, partitionCount: Int): Int {
        require(partitionCount > 0) { "partitionCount must be positive" }
        return (aggregateId.hashCode() and Int.MAX_VALUE) % partitionCount
    }
}
