package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.CdcEvent
import com.lab.mongodbcdckafka.producer.PartitionKeyResolver
import org.springframework.stereotype.Service

@Service
class OrderingGuaranteeService(
    private val partitionKeyResolver: PartitionKeyResolver,
) {

    fun partitionAssignments(events: List<CdcEvent<*>>, partitionCount: Int): Map<String, Int> =
        events.associate { event ->
            event.aggregateId to partitionKeyResolver.assignPartition(event.aggregateId, partitionCount)
        }

    fun isOrderedWithinAggregate(events: List<CdcEvent<*>>): Boolean =
        events
            .groupBy { it.aggregateId }
            .values
            .all { aggregateEvents ->
                aggregateEvents.zipWithNext().all { (current, next) -> current.version <= next.version }
            }

    fun validateConsumerConcurrency(partitionCount: Int, consumerConcurrency: Int): String =
        if (consumerConcurrency > partitionCount) {
            "consumer concurrency가 partition 수보다 커서 일부 스레드는 유휴 상태이며, 순서 보장 설명 시 오해를 부를 수 있습니다."
        } else {
            "aggregate key 기준 partitioning과 현재 concurrency 조합은 설명 가능한 수준입니다."
        }
}
