# MongoDB CDC + Kafka 증분 처리 파이프라인

> MongoDB 변경 이벤트를 Kafka로 전달해 증분 처리할 때 필요한 유실 검증, 순서 보장, 모델링, 소비 전략을 학습하는 주제

## 📌 개요

MongoDB CDC와 Kafka를 조합한 파이프라인은 검색 인덱스 갱신, 통계 집계, 캐시 동기화, 이벤트 기반 후처리에서 자주 사용됩니다. 다만 "이벤트를 흘린다" 수준으로 이해하면 면접이나 운영 상황에서 취약합니다. 유실 가능 지점, 순서 보장 범위, 중복 처리, MongoDB 모델링 기준까지 설명할 수 있어야 합니다.

이 주제는 MongoDB CDC + Kafka 증분 처리 파이프라인을 면접 관점과 실무 관점에서 함께 정리합니다. 실제 구현은 학습용 시뮬레이션에 집중하고, README와 테스트로 핵심 트레이드오프를 설명하는 형태로 구성했습니다.

## 🔍 문제 정의

### 언제 발생하는가?

- MongoDB 문서 변경을 다른 저장소나 뷰 모델에 비동기 반영할 때
- Change Stream 또는 Debezium 기반 CDC를 Kafka로 연결할 때
- 메시지 유실, 중복, 순서 역전이 서비스 품질에 직접 영향을 줄 때
- 면접에서 파이프라인 정합성 검증과 모델링 선택 이유를 설명해야 할 때

### 왜 문제인가?

- CDC는 "잘 흘러가는 것처럼 보이지만 일부가 유실"되는 구간이 생기기 쉽습니다.
- Kafka는 기본적으로 강한 내구성을 제공하지만, 설정과 소비 방식에 따라 유실처럼 보이는 상황이 생깁니다.
- 순서는 전역적으로 보장되지 않기 때문에 "어디까지 보장했는가"를 명확히 설명해야 합니다.
- MongoDB 모델링을 잘못 선택하면 문서 비대화, 갱신 비용 증가, 복잡한 조회로 이어집니다.

### 실제 사례

```kotlin
data class CdcEvent<T>(
    val resumeToken: Long,
    val aggregateId: String,
    val version: Long,
    val payload: T
)

fun processIncrementally(event: CdcEvent<Map<String, Any>>) {
    val partitionKey = event.aggregateId
    // 같은 aggregateId를 같은 partition으로 보내지 않으면
    // 동일 엔티티의 변경 순서가 뒤바뀔 수 있다.
    publishToKafka(key = partitionKey, payload = event)
}
```

## 💡 발생 원인

### 1. CDC 유실은 단일 지점 문제가 아니다

유실은 보통 한 곳에서만 생기지 않습니다.

- MongoDB change stream을 읽는 구간
- CDC connector가 Kafka에 적재하는 구간
- Kafka consumer가 offset을 commit하고 sink에 반영하는 구간

따라서 검증도 `source -> topic -> sink` 3구간으로 나눠서 봐야 합니다.

### 2. Kafka는 안전하지만 설정에 따라 손실 가능성이 생긴다

- `acks=0`
- 브로커 복제 미비
- 처리 전 offset 선커밋
- 예외를 삼키는 consumer 로직
- DLQ 부재

즉 Kafka 자체보다 애플리케이션 처리 방식에서 손실처럼 보이는 케이스가 많습니다.

### 3. 순서는 전역이 아니라 key 범위에서 설계한다

Kafka는 한 partition 내 순서는 유지하지만, 여러 partition 간 전역 순서는 보장하지 않습니다. 따라서 순서를 보장하려면 aggregate id 같은 비즈니스 키를 기준으로 partition을 설계해야 합니다.

### 4. MongoDB 모델링은 조회 패턴이 결정한다

Embed와 Reference의 선택은 정답 문제가 아니라 읽기/쓰기 패턴 문제입니다.

- 항상 함께 읽고 함께 바꾸며 크기가 제한되면 `Embed`
- 독립 조회가 많고 cardinality가 커질 수 있으면 `Reference`

## 🛠️ 해결 방법

### 방법 1: CDC 유실 검증 체계를 구간별로 나눈다

**설명:**
CDC 정합성을 단순 로그 확인으로 끝내지 않고, source / topic / sink별로 검증 포인트를 둡니다.

**장점:**
- 유실 발생 지점을 좁혀서 찾을 수 있음
- 운영 지표와 재처리 전략을 설계하기 쉬움

**단점:**
- 메트릭과 대사 배치(reconciliation) 설계가 추가로 필요함
- 실시간만으로 완전 검증이 어렵고 사후 검증 체계가 필요함

**언제 사용하는가:**
- CDC 파이프라인을 운영하거나 면접에서 검증 방법을 설명해야 할 때

**코드 예시:**
```kotlin
val gapReport = cdcVerificationService.detectGaps(events)
val countFindings = cdcVerificationService.reconcileCounts(
    CountSnapshot(sourceCount = 120, topicCount = 119, sinkCount = 118)
)
```

### 방법 2: aggregate key 기준으로 Kafka 순서를 설계한다

**설명:**
전역 순서 보장이 아니라 aggregate 단위 순서를 보장하도록, 동일 aggregate id를 항상 동일 partition으로 보냅니다.

**장점:**
- 설명 가능한 순서 보장 범위를 확보할 수 있음
- consumer scaling과 ordering 사이 트레이드오프를 명확히 할 수 있음

**단점:**
- hot key가 생기면 특정 partition 쏠림이 발생할 수 있음
- 서로 다른 aggregate 간 전역 순서는 여전히 보장되지 않음

**언제 사용하는가:**
- 주문, 회원, 상품 등 엔티티 단위 변경 이벤트를 처리할 때

**코드 예시:**
```kotlin
val partition = partitionKeyResolver.assignPartition(
    aggregateId = "order-123",
    partitionCount = 3
)
```

### 방법 3: consumer는 멱등성과 실패 경로를 함께 설계한다

**설명:**
Kafka는 at-least-once 소비가 일반적이므로, 중복 소비를 허용하고 처리 결과가 안전하도록 멱등성, retry, DLQ를 같이 설계합니다.

**장점:**
- 재시도와 장애 복구가 쉬움
- offset commit 전략을 설명하기 쉬움

**단점:**
- 중복 방지 키 또는 version 체크가 필요함
- 운영 시 DLQ 모니터링 절차가 필요함

**언제 사용하는가:**
- 증분 처리 파이프라인 전반

**코드 예시:**
```kotlin
if (projection.latestAppliedVersion >= event.version) {
    return // 이미 반영된 이벤트면 무시
}
```

### 방법 4: MongoDB 모델링은 조회 패턴 기준으로 Embed / Reference를 고른다

**설명:**
문서 구조가 아니라 실제 질의 패턴과 변경 단위를 기준으로 선택합니다.

**장점:**
- 모델링 선택 이유를 명확히 설명 가능
- 문서 크기, 원자성, 조회 비용을 함께 고려할 수 있음

**단점:**
- 도메인 성격이 바뀌면 모델링 재검토가 필요함
- Reference는 애플리케이션 조합 비용이 증가할 수 있음

**언제 사용하는가:**
- MongoDB 문서 구조를 설계할 때 항상

**코드 예시:**
```kotlin
val strategy = mongoModelingDecisionService.chooseStrategy(
    ModelingProfile(
        readTogether = true,
        updateTogether = true,
        boundedCardinality = true,
        independentlyQueriedChildren = false
    )
)
```

## 📊 해결 방법 비교

| 구분 | 구간별 검증 | Key 기반 순서 설계 | 멱등 소비 + DLQ | Embed / Reference 기준화 |
|------|-------------|--------------------|-----------------|--------------------------|
| **목적** | 유실 검증 | 순서 보장 | 중복/실패 대응 | 데이터 모델링 |
| **핵심 지표** | source/topic/sink count | partition key, version | offset, retry, DLQ | read/write pattern |
| **복잡도** | 중간 | 중간 | 높음 | 중간 |
| **운영 영향도** | 높음 | 높음 | 매우 높음 | 중간 |
| **면접 활용도** | 매우 높음 | 매우 높음 | 매우 높음 | 높음 |
| **러닝커브** | 중간 | 중간 | 높음 | 낮음 |

## 🚀 구현 체크리스트

- [x] CDC 유실 검증 시나리오 작성
- [x] Kafka 메시지 유실 조건 정리
- [x] aggregate 단위 순서 보장 코드 작성
- [x] MongoDB Embed / Reference 판단 코드 작성
- [x] Kotlin Scope Function 사용 가이드 작성
- [x] 통합 테스트 작성
- [x] 성능 비교 테스트 작성
- [x] report.md 작성
- [x] troubleshooting.md 작성

## 🔬 테스트 실행

```bash
cd topics/mongodb-cdc-kafka-pipeline/implementation
./gradlew test
```

## 📖 참고 자료

- [MongoDB Change Streams](https://www.mongodb.com/docs/manual/changeStreams/)
- [Debezium MongoDB Connector](https://debezium.io/documentation/reference/stable/connectors/mongodb.html)
- [Kafka Semantics](https://kafka.apache.org/documentation/#semantics)
- [Spring for Apache Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [MongoDB Data Modeling](https://www.mongodb.com/docs/manual/data-modeling/)

## 🔗 관련 주제

- [JPA N+1 문제](../jpa-n-plus-one/README.md)
- `kafka-fundamentals` (추가 예정)
- `mongodb-modeling-patterns` (추가 예정)
