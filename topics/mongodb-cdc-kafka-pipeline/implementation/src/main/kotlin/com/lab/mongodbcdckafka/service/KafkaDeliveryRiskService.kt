package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.KafkaDeliveryRisk
import com.lab.mongodbcdckafka.domain.PipelineStage
import org.springframework.stereotype.Service

@Service
class KafkaDeliveryRiskService {

    fun knownRisks(): List<KafkaDeliveryRisk> = listOf(
        KafkaDeliveryRisk(
            stage = PipelineStage.PRODUCER,
            condition = "acks=0 또는 전송 예외를 무시한 경우",
            consequence = "브로커 저장 전에 성공처럼 보일 수 있습니다.",
            mitigation = "acks=all, 재시도 설정, 전송 실패 로깅과 메트릭을 사용합니다.",
        ),
        KafkaDeliveryRisk(
            stage = PipelineStage.BROKER,
            condition = "복제되지 않은 상태에서 leader 장애가 나거나 위험한 leader election 설정을 둔 경우",
            consequence = "브로커 레벨 메시지 손실 가능성이 생깁니다.",
            mitigation = "replication factor, min.insync.replicas, 안전한 leader election 정책을 사용합니다.",
        ),
        KafkaDeliveryRisk(
            stage = PipelineStage.CONSUMER,
            condition = "실제 처리 전에 offset을 먼저 commit한 경우",
            consequence = "애플리케이션 레벨에서 메시지가 유실된 것처럼 보입니다.",
            mitigation = "처리 성공 후 commit, retry / DLQ, 멱등 처리를 적용합니다.",
        ),
        KafkaDeliveryRisk(
            stage = PipelineStage.CONSUMER,
            condition = "예외를 삼키고 DLQ도 남기지 않은 경우",
            consequence = "재처리 경로 없이 메시지가 사라집니다.",
            mitigation = "실패 이벤트를 DLQ로 보내고 재처리 절차를 문서화합니다.",
        ),
    )
}
