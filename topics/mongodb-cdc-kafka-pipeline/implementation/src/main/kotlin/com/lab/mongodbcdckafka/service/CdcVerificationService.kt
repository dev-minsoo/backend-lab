package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.CdcEvent
import com.lab.mongodbcdckafka.domain.CountSnapshot
import com.lab.mongodbcdckafka.domain.GapReport
import com.lab.mongodbcdckafka.domain.IncrementalProjection
import org.springframework.stereotype.Service

@Service
class CdcVerificationService {

    fun detectGaps(events: List<CdcEvent<*>>): GapReport {
        if (events.isEmpty()) {
            return GapReport(emptyList(), listOf("비교할 CDC 이벤트가 없습니다."))
        }

        val sortedTokens = events.map { it.resumeToken }.sorted()
        val missingTokens = mutableListOf<Long>()

        for (index in 1 until sortedTokens.size) {
            val previous = sortedTokens[index - 1]
            val current = sortedTokens[index]
            if (current > previous + 1) {
                missingTokens += ((previous + 1) until current)
            }
        }

        val findings = buildList {
            if (missingTokens.isEmpty()) {
                add("resume token 연속성은 유지되었습니다.")
            } else {
                add("resume token 누락이 감지되었습니다: $missingTokens")
            }
        }

        return GapReport(missingTokens, findings)
    }

    fun reconcileCounts(snapshot: CountSnapshot): List<String> {
        val findings = mutableListOf<String>()

        if (snapshot.sourceCount != snapshot.topicCount) {
            findings += "MongoDB source 변경 건수와 Kafka 적재 건수가 다릅니다."
        }

        if (snapshot.topicCount != snapshot.sinkCount) {
            findings += "Kafka 적재 건수와 최종 sink 반영 건수가 다릅니다."
        }

        if (findings.isEmpty()) {
            findings += "source, topic, sink 건수가 일치합니다."
        }

        return findings
    }

    fun verifyLatestVersion(
        events: List<CdcEvent<*>>,
        projections: List<IncrementalProjection>,
    ): List<String> {
        val latestVersionByAggregate = events
            .groupBy { it.aggregateId }
            .mapValues { (_, aggregateEvents) -> aggregateEvents.maxOf { it.version } }

        return latestVersionByAggregate.mapNotNull { (aggregateId, latestVersion) ->
            val projection = projections.find { it.aggregateId == aggregateId }
            when {
                projection == null -> "$aggregateId 에 대한 projection이 없습니다."
                projection.latestAppliedVersion < latestVersion ->
                    "$aggregateId 의 projection이 최신 버전 $latestVersion 까지 따라오지 못했습니다."
                else -> null
            }
        }
    }
}
