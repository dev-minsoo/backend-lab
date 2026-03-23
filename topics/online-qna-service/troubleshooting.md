# 온라인 QnA 서비스 - 문제 해결 과정

---

## Issue #1: 테스트 환경에서 Kafka와 Elasticsearch 없이도 전체 시나리오를 검증해야 함

**발생일:** 2026-03-23
**심각도:** 🟡 Medium
**소요 시간:** 40분

### 📌 상황

도커 인프라가 없는 상태에서도 `./gradlew test`가 돌아야 했습니다. 하지만 구현 요구사항에는 Kafka 이벤트, Elasticsearch 검색이 포함되어 있었습니다.

### 🐛 증상

- 외부 브로커/검색 서버가 없으면 테스트 컨텍스트가 쉽게 실패
- 핵심 도메인 테스트가 인프라 준비 여부에 종속됨

### 🔍 원인 분석

- 이벤트 발행과 검색 인덱스 갱신 로직이 외부 인프라에 직접 묶여 있으면 테스트 격리가 어려움

### ✅ 해결 방법

`EventPublisher`, `SearchIndexService`를 인터페이스로 분리하고, `test` 프로필에서는 inline/no-op 대체 구현을 사용했습니다.

```kotlin
@ConditionalOnProperty(name = ["app.messaging.enabled"], havingValue = "false")
class InlineEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : EventPublisher
```

### 💭 회고

- 시스템 설계 예제라도 테스트 격리는 반드시 필요
- 외부 인프라는 구조를 보여주되, 도메인 테스트는 독립적으로 유지하는 편이 학습에도 유리

---

## Issue #2: read/write 데이터소스 라우팅은 테스트에서도 안전하게 동작해야 함

**발생일:** 2026-03-23
**심각도:** 🟡 Medium
**소요 시간:** 20분

### 📌 상황

실행 환경에서는 MySQL primary/replica를 사용하지만 테스트에서는 H2 하나만 써야 했습니다.

### 🔍 원인 분석

- DataSource 라우팅은 유지하되, test 프로필에서는 write/read 모두 같은 H2를 바라보게 하면 됨

### ✅ 해결 방법

- `app.datasource.write`, `app.datasource.read`를 유지
- test 프로필에서는 둘 다 동일한 H2 URL 사용

### 💭 회고

- 운영 구조와 테스트 구조를 완전히 다르게 만들기보다, 설정만 바꿔 동일한 코드 경로를 유지하는 편이 안전

---

## Issue #3: 캐시 적용 시 리스트와 상세 캐시 무효화 전략이 다름

**발생일:** 2026-03-23
**심각도:** 🟢 Low
**소요 시간:** 15분

### 📌 상황

질문 수정/삭제/답변 등록 시 상세 캐시와 목록 캐시의 무효화 범위가 다릅니다.

### ✅ 해결 방법

- 상세는 질문 단건 기준으로 무효화
- 목록은 페이지별 키가 달라질 수 있어 전체 비우는 전략 사용

### 💭 회고

- 캐시는 붙이는 것보다 무효화 설계가 더 어렵다
- 실제 서비스에서는 version key, topic-based invalidation, event-driven cache busting까지 검토할 수 있음
