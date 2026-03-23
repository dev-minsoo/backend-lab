# 온라인 QnA 서비스 - 성능 분석 리포트

**작성일:** 2026-03-23
**작성자:** Codex
**실험 환경:** 로컬 개발 환경

---

## 🎯 실험 목적

### 검증하고자 하는 가설

1. 질문 상세 조회는 캐시 적용 시 반복 조회 응답 시간이 줄어든다.
2. 검색은 RDB 조회보다 ES 색인 기반 경로가 구조적으로 더 적합하다.
3. 알림과 검색 반영을 비동기 처리하면 쓰기 API 경로가 단순해진다.

### 측정 지표

- 질문 상세 반복 조회 시간
- 질문 목록 조회 경로의 DB 의존도
- 글 작성 API의 동기 책임 범위

## 🔧 실험 환경

### 소프트웨어 스펙

```yaml
Kotlin: 1.9.22
Spring Boot: 3.2.1
JVM: OpenJDK 17
Primary DB: MySQL 8
Read Replica: MySQL 8 Replica
Cache: Redis 7
Search: Elasticsearch 8.12
Message Broker: Kafka 3.7
```

### 데이터셋

- 사용자: 2명 이상
- 질문: 100건
- 답변: 질문별 0~1건
- 태그: redis, cache, kafka, elasticsearch, mysql

## 📋 테스트 시나리오

### 시나리오 1: 질문 상세 반복 조회

```kotlin
val coldRead = measureTimeMillis {
    repeat(30) {
        questionService.getQuestion(target.id)
    }
}

val warmRead = measureTimeMillis {
    repeat(30) {
        questionService.getQuestion(target.id)
    }
}
```

### 시나리오 2: 질문 생성 후 비동기 후처리

- 질문 저장
- 질문 변경 이벤트 발행
- 검색 인덱스 갱신
- 작성 알림 저장

## 📊 측정 결과

### 1. 구조적 비교

| 항목 | 동기 단일 DB 처리 | 현재 구현 |
|------|-------------------|-----------|
| 질문 쓰기 | 질문 저장 + 검색 반영 + 알림 저장 | 질문 저장 + 이벤트 발행 |
| 질문 상세 조회 | 항상 DB 조회 | Redis 캐시 우선 |
| 태그 검색 | RDB 조건 검색 | ES 색인 조회 |
| 읽기 저장소 | primary 단일 | primary/replica 분리 |

### 2. 관찰 결과

| 항목 | 결과 |
|------|------|
| 캐시 warm read | cold read 대비 더 짧거나 같음 |
| 검색 경로 | 질문 변경 이벤트 이후 인메모리/ES 색인 조회 가능 |
| 알림 생성 | 답변 등록, 답변 채택 시 비동기 이벤트로 분리 |

## 🔍 분석

### 주요 발견사항

1. **read-heavy 구조에서는 캐시와 replica가 같이 필요**
   캐시는 가장 자주 읽는 hot key를 줄이고, replica는 캐시에 없는 일반 read 트래픽을 분산합니다.

2. **검색은 DB 부하와 별개로 분리하는 편이 낫다**
   태그 검색, 키워드 검색, 정렬/필터 조합은 ES가 훨씬 자연스럽습니다.

3. **비동기 처리는 eventual consistency를 전제로 해야 한다**
   질문이 저장된 직후 검색 결과와 알림이 아주 잠깐 늦을 수 있습니다. 대신 쓰기 API는 더 단순하고 빨라집니다.

## 💡 결론

### 권장사항

- 질문 상세/목록 같은 핵심 조회 API는 Redis 캐시를 우선 검토
- read-heavy 서비스는 primary/replica 분리를 기본 옵션으로 고려
- 검색은 조기부터 ES로 분리하는 것이 확장성에 유리
- 알림, 검색 색인, 통계는 Kafka 기반 비동기 후처리로 분리

### 다음 단계

- 인기 질문 랭킹을 Redis Sorted Set으로 분리
- replica lag 대응용 primary fallback 전략 추가
- Kafka dead letter topic, retry 정책 추가
- Testcontainers 기반 통합 테스트 확장
