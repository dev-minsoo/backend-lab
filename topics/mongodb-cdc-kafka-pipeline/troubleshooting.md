# MongoDB CDC + Kafka 증분 처리 파이프라인 - 문제 해결 과정

---

## Issue #1: CDC 유실을 어떻게 검증해야 할지 설명이 모호함

**발생일:** 2026-03-30
**심각도:** 🟡 Medium
**소요 시간:** 1시간

### 📌 상황

면접에서 "CDC 유실이 발생할 수 있을 것 같은데 어떻게 검증했나요?"라는 질문을 받았지만, Kafka에 이벤트가 들어가는지만 보는 수준에서 답변이 멈췄다.

### 🐛 증상

- source와 sink 사이 어느 구간을 봐야 하는지 설명하지 못함
- resume token, offset, reconciliation 개념이 섞여서 답변이 흐려짐

### 🔍 원인 분석

실시간 이벤트 파이프라인을 한 덩어리로 이해하고 있었다. 실제로는 아래 3구간으로 쪼개서 봐야 했다.

1. MongoDB source 변경 발생
2. Kafka topic 적재
3. Consumer 처리 후 sink 반영

### ✅ 해결 방법

검증 기준을 구간별로 분리했다.

```kotlin
val gapReport = cdcVerificationService.detectGaps(events)
val findings = cdcVerificationService.reconcileCounts(
    CountSnapshot(sourceCount = 120, topicCount = 119, sinkCount = 118)
)
```

정리한 답변 포인트:

- resume token 연속성 확인
- source / topic / sink 건수 대사
- aggregate 최신 버전 반영 여부 확인
- DLQ 적재량과 재처리 건수 모니터링

### 💭 회고

CDC 유실 질문은 "100% 유실 없음"을 주장하는 질문이 아니라, "유실 가능 구간을 어떻게 나눠 검증했는가"를 묻는 질문에 가깝다.

---

## Issue #2: Kafka에서 메시지 유실이 언제 생기는지 설명이 약함

**발생일:** 2026-03-30
**심각도:** 🟡 Medium
**소요 시간:** 40분

### 📌 상황

Kafka가 안전하다는 인상만 갖고 있었고, 어떤 조건에서 유실이 생기거나 유실처럼 보이는지 체계적으로 정리하지 못했다.

### 🔍 원인 분석

Producer / Broker / Consumer 구간을 나누지 않고 한 번에 설명하려다 핵심을 놓쳤다.

### ✅ 해결 방법

리스크를 세 구간으로 정리했다.

- Producer: `acks=0`, 전송 실패 무시
- Broker: 복제 부족 상태의 장애
- Consumer: 처리 전 offset commit, 예외 삼키기, DLQ 부재

### 💭 회고

"Kafka 메시지 유실"은 브로커 손실만 뜻하지 않는다. 애플리케이션 레벨 손실까지 포함해서 설명해야 실무적이다.

---

## Issue #3: MongoDB Embed / Reference 선택 기준이 추상적이었음

**발생일:** 2026-03-30
**심각도:** 🟢 Low
**소요 시간:** 30분

### 📌 상황

면접에서 어떤 기준으로 임베드와 레퍼런스를 나눴는지 물었을 때, "상황에 따라 다릅니다" 수준으로 답했다.

### ✅ 해결 방법

판단 기준을 아래 네 가지로 고정했다.

1. 함께 읽는가
2. 함께 수정하는가
3. cardinality가 제한적인가
4. 자식이 독립 조회되는가

```kotlin
val strategy = mongoModelingDecisionService.chooseStrategy(
    ModelingProfile(
        readTogether = true,
        updateTogether = true,
        boundedCardinality = true,
        independentlyQueriedChildren = false,
    )
)
```

### 💭 회고

정답을 찾기보다, 읽기/쓰기 패턴 기준으로 일관된 판단 틀을 갖는 것이 더 중요했다.
