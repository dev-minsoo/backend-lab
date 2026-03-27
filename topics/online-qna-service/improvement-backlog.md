# 온라인 QnA 서비스 개선 백로그

이 문서는 현재 구현을 기준으로, 대화 중 확인했지만 바로 반영하지 않고 넘긴 개선 포인트를 정리한 목록입니다.  
하나씩 선택해서 후속 작업으로 처리할 수 있도록 우선순위와 관련 파일을 함께 적었습니다.

## 완료된 항목

### 1. 목록 캐시 eviction 정책 수정

- 완료 커밋: `23cf222`
- 반영 내용:
  - `questionDetail`은 `questionId` 기반으로 개별 eviction
  - `questionList`는 `allEntries = true`로 전체 eviction
  - `@Caching`으로 정책 분리

### 2. vote 집계 쿼리 최적화

- 완료 커밋: `c0d0284`
- 반영 내용:
  - `VoteRepository`에 `GROUP BY targetId, type` 집계 쿼리 추가
  - `VoteService.summarizeAll(...)` 추가
  - 질문 목록/내 질문 목록/내 답변 목록/질문 상세 답변 집계에 배치 요약 적용

## 빠르게 손볼 수 있는 항목

## 3. Kafka UI 외 운영 편의 문서 보강

### 현재 상태

Kafka UI는 붙였지만 운영/실습 관찰 포인트가 더 체계화될 수 있습니다.

### 개선 방향

- 토픽별 메시지 예시
- consumer group 확인 포인트
- lag 해석 가이드
- 알림/검색 이벤트 흐름 시퀀스 정리

### 관련 파일

- [README.md](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/README.md)
- [practice-checklist.md](/Users/q/projects/playground/backend-lab/topics/online-qna-service/practice-checklist.md)

## 중간 난이도 확장 항목

## 4. Kafka DLQ / retry / backoff 적용

### 현재 상태

역직렬화 오류는 consumer factory 분리로 해결했지만, 운영 수준의 retry / DLQ 정책은 없습니다.

### 문제

- poison message가 생기면 운영자가 추적/복구하기 어려움
- 실패 메시지를 따로 보관할 공간이 없음

### 개선 방향

- `DefaultErrorHandler` + backoff
- Dead Letter Topic 추가
- 재처리 전략 명시

### 관련 파일

- [KafkaConfig.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/config/KafkaConfig.kt)

## 5. 인기 질문 랭킹 구현

### 현재 상태

README에는 Redis ZSET 기반 아이디어만 있고 실제 구현은 없습니다.

### 문제

- 요구사항의 "좋아요순 / 인기순"이 문서 수준에 머무름

### 개선 방향

- Redis ZSET key 설계
- `ZINCRBY` 기반 score 반영
- Top N 조회 API
- 상세 정보 보강 로직

### 관련 파일

- 신규 서비스/컨트롤러 필요
- [VoteService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/VoteService.kt)

## 6. Elasticsearch 검색 품질 향상

### 현재 상태

현재는 기본 `multi_match + term filter` 수준입니다.

### 문제

- 오타 보정 미흡
- 자동완성 없음
- analyzer / synonym 튜닝 없음
- `_class` 필드 제거 등 매핑 정리가 덜 됨

### 개선 방향

- fuzzy search
- analyzer 커스터마이징
- autocomplete
- 명시적 mapping

### 관련 파일

- [ElasticSearchIndexService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/ElasticSearchIndexService.kt)
- [SearchDtos.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/dto/SearchDtos.kt)

## 7. 읽기 일관성 보강

### 현재 상태

`@Transactional(readOnly = true)` 기준으로 replica 라우팅만 합니다.

### 문제

- write 직후 read 시 replica lag 영향 가능
- read-after-write 보장 필요 API에서 오동작 가능

### 개선 방향

- primary fallback
- lag 감지
- 특정 API는 강제 primary read

### 관련 파일

- [DataSourceConfig.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/config/DataSourceConfig.kt)
- 서비스 계층 `@Transactional` 설계

## 더 큰 구조 개선 항목

## 8. Outbox Pattern 도입

### 현재 상태

현재는 `afterCommit` 이후 Kafka publish 방식입니다.

### 장점

- race condition은 줄였음

### 한계

- DB commit 이후 Kafka publish에서 실패하면 이벤트 유실 가능
- 메시지 전달 보장 수준은 Outbox보다 약함

### 개선 방향

- outbox 테이블 저장
- 별도 퍼블리셔로 Kafka 전송
- 이벤트 전달 안정성 강화

### 관련 파일

- [KafkaEventPublisher.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/KafkaEventPublisher.kt)

## 9. anti-abuse / rate limit / 스팸 방지

### 현재 상태

인증/인가 외의 스팸 방지 장치는 아직 없습니다.

### 문제

- 로그인 시도 제한 없음
- 질문/답변 도배 방지 없음
- 악성 트래픽 제어 부족

### 개선 방향

- Redis 기반 rate limit
- 사용자별/아이피별 제한
- CAPTCHA 등 확장

### 관련 파일

- 신규 필터 또는 인터셉터
- Redis 관련 서비스

## 우선순위 추천

### 1순위

- Kafka UI 외 운영 편의 문서 보강
- Kafka DLQ / retry
- 인기 질문 Redis ZSET 구현

### 2순위

- ES 검색 품질 향상
- 읽기 일관성 보강
- Outbox Pattern

### 3순위

- anti-abuse / rate limit
