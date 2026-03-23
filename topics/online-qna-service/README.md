# 온라인 QnA 서비스 시스템 설계

> read-heavy QnA 서비스를 MySQL primary/replica, Redis, Elasticsearch, Kafka로 구현하며 조회 경로 최적화와 비동기 처리를 함께 학습하는 주제

## 📌 개요

온라인 QnA 서비스는 질문/답변 등록 자체보다 조회가 훨씬 많은 대표적인 read-heavy 서비스입니다. 질문 상세, 목록, 인기 질문, 태그 검색, 알림 같은 기능을 단순 CRUD로만 구현하면 트래픽이 늘수록 DB 읽기 부하와 응답 지연이 빠르게 커집니다.

이 주제는 단순 게시판 구현이 아니라, 실무형 시스템 설계 포인트를 코드로 확인하는 데 목적이 있습니다. 특히 `write/read DB 분리`, `Redis 캐시`, `Elasticsearch 색인`, `Kafka 비동기 이벤트`, `저장소 계층 확장`을 한 번에 경험할 수 있게 구성했습니다.

## 🔍 문제 정의

### 언제 발생하는가?

- DAU 10만 규모에서 질문/답변 조회가 쓰기보다 압도적으로 많을 때
- 질문 상세와 목록이 서비스의 핵심 read path일 때
- 태그 검색, 인기 질문, 알림 같은 부가 기능이 핵심 트랜잭션을 느리게 만들 때
- 애플리케이션 replica는 쉽게 늘릴 수 있지만 DB 단일 노드가 병목이 되기 시작할 때

### 왜 문제인가?

1. 질문/답변 상세 조회가 모두 primary DB로 몰리면 쓰기와 읽기가 서로 영향을 줍니다.
2. 태그 검색을 RDB LIKE 쿼리로 처리하면 데이터가 늘수록 응답 시간이 악화됩니다.
3. 알림, 검색 색인 반영을 동기 처리하면 글 작성 API 지연이 커집니다.
4. 캐시가 없으면 인기 질문 한 개가 DB에 반복 타격을 줍니다.
5. 저장소 계층 확장 전략이 없으면 API 서버만 늘려도 전체 성능은 좋아지지 않습니다.

### 실제 사례

```kotlin
@Transactional(readOnly = true)
@Cacheable(cacheNames = ["questionDetail"], key = "#questionId")
fun getQuestion(questionId: Long): QuestionDetailResponse {
    val question = questionRepository.findActiveQuestionById(questionId)
        .orElseThrow { DomainException("질문을 찾을 수 없습니다.") }

    val answers = answerRepository.findActiveAnswersByQuestionId(questionId)
        .map { it.toSummary(voteService.summarize(VoteTargetType.ANSWER, it.id!!)) }

    return question.toDetail(
        voteService.summarize(VoteTargetType.QUESTION, questionId),
        answers
    )
}
```

위 메서드는 다음 포인트를 같이 보여줍니다.

- `readOnly = true` 트랜잭션으로 read replica 라우팅
- `@Cacheable`로 Redis 캐시 적용
- 상세 조회는 DB에서 정확한 모델을 읽고, 검색은 ES에 위임

## 💡 발생 원인

### 1. 읽기 경로 병목

질문 상세, 목록, 프로필, 태그 검색은 모두 읽기 경로입니다. 이 경로를 모두 primary DB에 의존하면 write 성능까지 같이 흔들립니다.

### 2. 관심사 혼합

질문 등록 시 검색 인덱스 반영, 알림 생성, 통계 반영까지 한 트랜잭션에 몰아넣으면 API 응답이 불필요하게 느려집니다.

### 3. 저장소 역할 미분리

RDB는 정합성 있는 트랜잭션에 강하지만, 전문 검색과 초저지연 캐시까지 한 번에 가장 잘하는 저장소는 아닙니다. 목적별 저장소 분리가 필요합니다.

## 🛠️ 해결 방법

### 방법 1: MySQL primary/replica 분리

**설명:**
쓰기 트랜잭션은 primary, 읽기 전용 트랜잭션은 replica로 라우팅합니다.

**장점:**
- 읽기 부하를 저장소 계층에서 분산 가능
- API 서버 replica를 늘릴 때 read DB도 함께 확장 가능
- primary를 핵심 쓰기 트랜잭션에 집중시킬 수 있음

**단점:**
- replication lag 고려 필요
- read-after-write 일관성이 필요한 API는 primary fallback 전략이 필요할 수 있음
- 운영 복잡도가 증가함

**언제 사용하는가:**
- read-heavy 서비스
- 질문 목록/상세/프로필 조회 같은 read API가 많을 때

**코드 예시:**
```kotlin
override fun determineCurrentLookupKey(): Any =
    if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
        DataSourceType.READ
    } else {
        DataSourceType.WRITE
    }
```

### 방법 2: Redis 캐시로 상세/목록 조회 최적화

**설명:**
반복 조회가 많은 질문 상세와 목록 API에 Cache Aside 패턴을 적용합니다.

**장점:**
- 인기 질문 상세 조회를 빠르게 응답 가능
- DB read 부하 감소
- 애플리케이션 replica가 늘어나도 중앙 캐시로 효과 유지

**단점:**
- 무효화 전략 필요
- 짧은 시간 stale data 허용 필요

**언제 사용하는가:**
- 읽기 비율이 높은 API
- 쓰기 직후 완전한 동기 반영이 필수는 아닌 경우

**코드 예시:**
```kotlin
@Transactional(readOnly = true)
@Cacheable(cacheNames = ["questionList"], key = "#page + ':' + #size")
fun getQuestions(page: Int, size: Int): List<QuestionSummary> = ...
```

### 방법 3: Elasticsearch 비동기 색인

**설명:**
질문 생성/수정/삭제 시 Kafka 이벤트를 발행하고, 소비자가 ES 색인을 갱신합니다.

**장점:**
- 검색 기능을 DB 트랜잭션과 분리
- 태그 검색과 키워드 검색에 적합
- eventual consistency를 활용해 쓰기 지연 최소화

**단점:**
- 색인 지연이 있을 수 있음
- 운영해야 할 저장소가 늘어남

**언제 사용하는가:**
- 태그 기반 검색, 키워드 검색, 전문 검색이 필요한 경우

**코드 예시:**
```kotlin
override fun publishQuestionChanged(event: QuestionChangedEvent) {
    kafkaTemplate.send(appProperties.topics.questionChanged, event.questionId.toString(), event)
}

fun onQuestionChanged(event: QuestionChangedEvent) {
    questionRepository.findActiveQuestionById(event.questionId)
        .ifPresent { searchIndexService.upsert(it.toSearchDocument()) }
}
```

### 방법 4: 알림을 비동기 이벤트로 분리

**설명:**
질문 등록, 답변 등록, 답변 채택 시 알림을 동기 저장하지 않고 Kafka 이벤트로 넘겨 소비자가 알림 테이블에 적재합니다.

**장점:**
- 핵심 API 응답 시간 단축
- 알림 로직 확장이 쉬움
- 나중에 이메일/푸시/SSE로 자연스럽게 확장 가능

**단점:**
- 알림은 즉시 반영되지 않을 수 있음
- 이벤트 실패 처리 전략이 필요

**언제 사용하는가:**
- eventual consistency 허용 가능한 알림, 피드, 후처리 작업

## 📊 해결 방법 비교

| 구분 | Primary/Replica | Redis Cache | Elasticsearch + Kafka | Async Notification |
|------|-----------------|-------------|------------------------|-------------------|
| **주요 목적** | 읽기 저장소 분산 | 초저지연 조회 | 검색 최적화 | 부가 기능 분리 |
| **쿼리/부하 감소 효과** | 높음 | 매우 높음 | DB 검색 부하 제거 | 직접적 감소는 낮음 |
| **응답 시간 개선** | 중간 | 매우 높음 | 검색 API 높음 | 쓰기 API 높음 |
| **정합성 수준** | replica lag 존재 | stale cache 가능 | eventual consistency | eventual consistency |
| **복잡도** | 중간 | 중간 | 높음 | 중간 |
| **운영 비용** | 중간 | 중간 | 높음 | 중간 |
| **학습 포인트** | 저장소 확장 | 캐시 무효화 | 색인/이벤트 | 비동기 아키텍처 |

## 저장소 계층 확장이란?

사용자가 궁금해한 "애플리케이션 replica는 늘리면 되는데 저장소 계층 확장은 무엇인가"에 대한 답은 아래와 같습니다.

1. **read replica 추가**
   primary 1대가 쓰기를 담당하고, replica 여러 대가 읽기를 담당합니다.
2. **역할별 저장소 분리**
   정합성 데이터는 MySQL, 캐시는 Redis, 검색은 Elasticsearch로 분리합니다.
3. **파티셔닝/샤딩**
   데이터가 더 커지면 질문/사용자 데이터를 키 기준으로 DB 여러 대에 분산합니다.
4. **비동기 파이프라인**
   검색, 알림, 집계는 Kafka 소비자로 분리해 primary DB의 동기 부담을 줄입니다.

즉, 저장소 계층 확장은 "DB 서버 한 대 스펙업"이 아니라, 읽기/쓰기 분리, 저장소 역할 분리, 데이터 분산, 비동기 후처리까지 포함하는 개념입니다.

## 🚀 구현 체크리스트

- [x] 회원가입 / 로그인
- [x] 질문 CRUD
- [x] 답변 CRUD
- [x] 답변 채택
- [x] 질문/답변 좋아요, 싫어요
- [x] 태그 기반 질문 검색
- [x] 글 작성/채택 알림
- [x] 글 신고 기능
- [x] 사용자 프로필 조회
- [x] MySQL write/read 라우팅
- [x] Redis 캐시 적용
- [x] Kafka 비동기 이벤트 분리
- [x] Elasticsearch 검색 인덱싱
- [x] 통합 테스트 및 성능 테스트

## 🔬 테스트 실행

```bash
cd topics/online-qna-service/implementation
./gradlew test
```

## 📖 참고 자료

- [MySQL Replication](https://dev.mysql.com/doc/refman/8.0/en/replication.html)
- [Spring Cache Reference](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/reference/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/)
- [Elasticsearch Guide](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)

## 🔗 관련 주제

- [JPA N+1 문제](../jpa-n-plus-one/README.md)
- [Redis 종합편](../redis/README.md)
