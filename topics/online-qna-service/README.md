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

#### read/write 분리로 기대할 수 있는 점

이 구조는 캐시처럼 단건 요청의 레이턴시를 극적으로 줄이는 기술이라기보다, read-heavy 서비스에서 전체 처리량과 안정성을 높이는 데 더 가깝습니다.

1. **primary를 쓰기에 집중시킬 수 있음**
   질문 등록, 답변 등록, 투표, 채택 같은 쓰기 트랜잭션이 조회 트래픽과 직접 경쟁하지 않게 됩니다.

2. **읽기 트래픽을 replica로 분산할 수 있음**
   질문 목록, 상세, 프로필, 검색 보조 조회처럼 반복되는 read path를 replica가 받아서 primary 병목을 줄입니다.

3. **connection pool을 역할별로 분리할 수 있음**
   read 요청이 많아져도 조회용 커넥션 사용량이 write 커넥션을 잠식하지 않으므로 피크 시간대에 더 안정적입니다.

4. **피크 시간대의 throughput을 높이기 유리함**
   동시 접속이 많고 조회 비중이 높은 구간에서 더 많은 요청을 안정적으로 처리할 수 있습니다.

5. **수평 확장 여지가 생김**
   replica를 추가하면 read capacity를 점진적으로 늘릴 수 있습니다. 이후에는 DB 프록시나 애플리케이션 라우팅으로 여러 replica에 분산할 수 있습니다.

#### 다만 같이 알아야 할 제약

1. **read-after-write 불일치가 생길 수 있음**
   primary에 쓰자마자 replica에서 조회하면 아직 반영되지 않았을 수 있습니다.

2. **replication lag는 항상 0이 아님**
   로컬에서는 보통 매우 짧지만, 부하가 걸리면 수백 ms에서 수 초 이상까지도 벌어질 수 있습니다.

3. **단건 조회 레이턴시 개선이 핵심은 아님**
   캐시는 hot key를 빠르게 만드는 기술이고, primary/replica 분리는 저장소 부하를 분산해 전체 시스템을 덜 무너지게 만드는 기술입니다.

4. **트랜잭션 경계가 더 중요해짐**
   이 프로젝트처럼 `readOnly` 여부로 라우팅하는 구조에서는 서비스 계층의 `@Transactional`, `@Transactional(readOnly = true)` 의도가 명확해야 합니다.

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

#### Redis 캐시 적용으로 기대할 수 있는 점

이 구조는 primary/replica처럼 전체 읽기 부하를 분산하는 전략이라기보다, hot key의 응답 시간을 직접 줄이고 반복 조회를 Redis가 대신 받도록 만드는 데 더 가깝습니다.

1. **반복 조회 레이턴시를 크게 줄일 수 있음**
   질문 상세나 첫 페이지 목록처럼 반복 호출되는 API는 첫 조회 이후 Redis에서 바로 응답하므로 DB 왕복 비용을 줄일 수 있습니다.

2. **primary와 replica의 조회 부담을 함께 줄일 수 있음**
   캐시 hit가 나면 아예 DB를 타지 않기 때문에, read replica가 담당하던 조회 일부도 Redis가 흡수합니다.

3. **hot key에 특히 강함**
   인기 질문, 첫 페이지 목록, 자주 방문되는 프로필처럼 동일한 키가 여러 번 조회되는 경우 효과가 큽니다.

4. **애플리케이션 인스턴스가 늘어나도 같은 캐시를 공유할 수 있음**
   로컬 메모리 캐시와 달리 Redis는 중앙 캐시이므로 여러 replica가 동일한 캐시를 함께 사용할 수 있습니다.

5. **애너테이션 기반으로 빠르게 적용할 수 있음**
   이 프로젝트처럼 `@Cacheable`, `@CacheEvict`만으로 기본 Cache Aside 패턴을 선언형으로 구현할 수 있습니다.

#### 다만 같이 알아야 할 제약

1. **캐시 무효화가 가장 어려운 문제임**
   질문 상세처럼 키가 명확한 캐시는 비교적 쉽지만, 페이지네이션 목록 캐시는 어떤 페이지가 영향을 받는지 계산하기 어려워 전체 eviction 전략이 필요할 수 있습니다.

2. **stale data를 허용하는 전제가 필요함**
   쓰기 직후 매우 짧은 시간 동안 이전 캐시값이 남아 있을 수 있습니다. 이 프로젝트는 이런 eventual consistency를 일부 허용하는 방향입니다.

3. **첫 조회는 여전히 DB를 탐**
   cold read는 캐시 miss이므로 DB 조회 후 캐시에 적재됩니다. 캐시 효과는 warm read에서 드러납니다.

4. **복잡한 캐시 전략은 애너테이션만으로 부족할 수 있음**
   단순 조회 캐시는 `@Cacheable`이 편하지만, 조건부 저장, 부분 갱신, Sorted Set/Hash 같은 자료구조 활용은 `RedisTemplate`이 더 유연합니다.

5. **목록 캐시는 키 설계를 잘해야 함**
   이 프로젝트처럼 `page:size` 키를 쓰면 페이지별 캐시가 가능하지만, 정렬 조건, 필터, 검색 조건이 늘어나면 키 설계도 함께 복잡해집니다.

#### 이 프로젝트에서 사용한 캐싱 전략

이 프로젝트는 **Cache Aside 패턴**을 사용합니다.

흐름은 아래와 같습니다.

1. API가 먼저 캐시를 조회
2. 캐시 hit면 Redis 값을 바로 반환
3. 캐시 miss면 DB를 조회
4. 조회 결과를 Redis에 저장
5. 질문/답변 변경 시 관련 캐시를 eviction

스프링에서는 이를 `@Cacheable`, `@CacheEvict` 애너테이션으로 선언형으로 구현했습니다.  
즉 전략 자체는 Cache Aside이고, 구현 방식만 스프링 캐시 추상화를 쓴 것입니다.

#### hot key란?

hot key는 **유난히 많이 조회되는 특정 캐시 키**입니다.

예를 들면:

- `questionDetail::1`
- `questionList::0:20`

같은 키가 매우 자주 요청되면, DB 대신 Redis가 그 요청을 계속 받아주므로 성능 이점이 큽니다.  
반대로 Redis 입장에서는 특정 키에 요청이 몰리기 때문에, hot key가 심하면 Redis도 병목이 될 수 있습니다.

#### Redis가 병목이 되면 어떻게 하나?

1. **TTL 분산**
   모든 키가 같은 시점에 만료되지 않도록 랜덤 TTL을 섞어 cache avalanche를 줄입니다.

2. **local cache + Redis 2계층**
   정말 자주 읽히는 hot key는 JVM 로컬 캐시에도 잠깐 두어 Redis 부담을 추가로 줄일 수 있습니다.

3. **key 분산과 Cluster 도입**
   데이터 양과 트래픽이 커지면 Redis Cluster로 키를 여러 노드에 분산할 수 있습니다.

4. **stampede 방지**
   캐시 miss가 동시에 몰릴 때 락, single-flight, background refresh 같은 패턴으로 DB 폭주를 막을 수 있습니다.

5. **캐시 대상 축소**
   응답 전체를 캐싱하기보다 필요한 DTO만 저장해 payload 크기와 네트워크 부하를 줄일 수 있습니다.

#### Redis Sentinel과 Redis Cluster

Redis 운영 구조는 크게 Sentinel과 Cluster를 구분해서 이해하는 편이 좋습니다.

##### Redis Sentinel

Sentinel은 **고가용성(HA)과 failover** 중심의 구성입니다.

- master를 감시
- master 장애 시 replica 하나를 새 master로 승격
- 클라이언트가 새 master를 찾을 수 있게 지원

즉 Sentinel은 **장애가 나도 Redis 서비스를 계속 유지하게 하는 운영 구성**입니다.  
데이터 샤딩이나 수평 확장은 핵심 목적이 아닙니다.

##### Redis Cluster

Cluster는 **샤딩 + failover + 수평 확장**을 함께 가져가는 구조입니다.

- key를 여러 master 노드에 분산 저장
- 각 master에 replica를 붙여 장애 시 failover 가능
- 데이터 용량과 트래픽을 여러 노드로 나눔

즉 Cluster는 "안 죽게 운영"뿐 아니라, **데이터와 트래픽을 분산해 scale-out하는 구조**입니다.

##### 샤딩(sharding)이란?

샤딩은 데이터를 한 서버에 다 넣지 않고 **여러 노드에 나눠 저장하는 것**입니다.

예를 들어:

- 노드 A가 일부 키를 담당
- 노드 B가 일부 키를 담당
- 노드 C가 나머지 키를 담당

이렇게 나누면:

- 저장 공간이 분산되고
- 읽기/쓰기 부하도 분산되고
- 한 노드가 감당해야 하는 트래픽이 줄어듭니다

Redis Cluster는 내부적으로 key를 hash slot으로 나누어 어느 노드가 어떤 key를 맡을지 결정합니다.

#### Sentinel과 Cluster를 어떻게 구분해 선택하나

1. **장애 대응이 우선이고 데이터가 한 노드에 충분히 들어가면 Sentinel**
   즉 failover가 목적일 때 적합합니다.

2. **데이터 용량과 트래픽이 커져 여러 노드로 분산해야 하면 Cluster**
   즉 샤딩과 scale-out이 필요한 경우에 적합합니다.

이 프로젝트 규모에서는 단일 Redis만으로도 학습은 충분하지만, 실제 운영에서 트래픽이 더 커지면 Sentinel 또는 Cluster를 선택하는 고민으로 이어질 수 있습니다.

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
