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

#### 인기 질문 리스트는 어떻게 구현할 수 있는가

요구사항에 있는 "인기 질문 리스트 제공(좋아요 순, 최신 순 등)"은 정렬 기준에 따라 구현 방식이 달라집니다.

##### 최신순

최신순은 가장 단순합니다.

- MySQL에서 `created_at desc` 정렬
- 일반 목록 조회와 같은 read path에서 처리 가능

즉 최신순은 별도 랭킹 저장소 없이도 구현 가능합니다.

##### 좋아요순 / 인기순

좋아요순이나 인기순은 단순 정렬보다 "집계된 점수"가 필요합니다.

예를 들어:

- 좋아요 수
- 싫어요 수
- 답변 수
- 조회수
- 최신성 가중치

같은 값을 바탕으로 score를 계산할 수 있습니다.

이런 랭킹에는 Redis **Sorted Set(ZSET)** 이 매우 잘 맞습니다.

#### 왜 Redis ZSET이 랭킹에 적합한가

ZSET은 `member + score` 구조를 가지며, score 기준 정렬을 유지합니다.

예:

- member: `question:123`
- score: `57`

좋아요가 하나 올라가면:

```text
ZINCRBY questions:popular 1 question:123
```

의미:

- `questions:popular` ZSET에서
- `question:123`의 점수를 1만큼 증가
- 기존 점수가 있으면 증가, 없으면 새로 생성

상위 20개 인기 질문을 가져오려면:

```text
ZREVRANGE questions:popular 0 19
```

의미:

- score가 높은 순으로
- 0번째부터 19번째까지
- 즉 상위 20개 member 반환

#### Elasticsearch 랭킹과 Redis 랭킹은 다름

Elasticsearch도 `_score` 기반 랭킹이 있지만, 그건 **검색 연관도(relevance)** 랭킹입니다.

예:

- 검색어가 title에 잘 맞는가
- content에 얼마나 많이 등장하는가
- BM25 기준으로 얼마나 관련성이 높은가

반면 Redis ZSET 랭킹은 **비즈니스 점수(business score)** 랭킹입니다.

예:

- 좋아요 순
- 조회수 순
- 운영자가 정의한 인기 점수 순

즉:

- 검색 결과 relevance ranking = Elasticsearch
- 인기 질문 / 랭킹 보드 = Redis ZSET

이렇게 역할이 다릅니다.

#### 랭킹용 ZSET에 TTL을 두나

보통은 **일반 캐시처럼 무조건 TTL을 걸지 않는 경우가 많습니다.**

이유:

- 랭킹은 단순 캐시라기보다 집계 데이터 성격이 강함
- 만료되면 인기 질문 리스트가 통째로 사라질 수 있음
- 재계산 비용이 클 수 있음

그래서 자주 쓰는 방식은 두 가지입니다.

1. **전체 랭킹 키를 TTL 없이 유지**
   예: `questions:popular`

2. **기간별 랭킹 키를 별도로 두고 TTL 적용**
   예:
   - `questions:popular:daily:2026-03-25`
   - `questions:popular:weekly:2026-W13`

기간별 키는 "오늘 인기", "이번 주 인기" 구현에도 유리하고, TTL로 자연스럽게 정리할 수 있습니다.

#### 이 프로젝트에 적용한다면

학습용 확장 시나리오로는 아래가 자연스럽습니다.

1. **최신순**
   MySQL `created_at desc`

2. **좋아요순**
   Redis ZSET score를 like count 기반으로 관리

3. **인기순**
   `좋아요 * 가중치 + 답변 수 * 가중치 - 싫어요 * 가중치` 같은 score를 Redis ZSET에 저장

즉:

- 원본 데이터: MySQL
- 반복 조회 캐시: Redis Cache
- 인기 랭킹: Redis ZSET
- 검색: Elasticsearch

로 역할을 나눌 수 있습니다.

#### SWR(Stale-While-Revalidate)

SWR은 **일단 오래된 캐시라도 빠르게 반환하고, 뒤에서 새 값을 다시 갱신하는 전략**입니다.

흐름:

1. 캐시가 만료되었더라도 즉시 stale 값을 반환
2. 사용자 응답은 빠르게 끝냄
3. 백그라운드에서 최신 데이터를 다시 읽어 캐시 갱신

장점:

- hot key가 만료되는 순간에도 응답 지연을 줄일 수 있음
- cache miss가 한꺼번에 몰리는 현상을 완화 가능

단점:

- 사용자가 잠깐 오래된 데이터를 볼 수 있음
- 구현이 단순 `@Cacheable`보다 복잡함

이 프로젝트는 현재 기본 Cache Aside만 사용하지만, 인기 질문 상세나 첫 페이지 목록처럼 트래픽이 매우 높은 키에는 SWR이 좋은 다음 단계가 될 수 있습니다.

#### 캐시 관통(Cache Penetration)

캐시 관통은 **존재하지 않는 데이터에 대한 요청이 반복되어, 캐시 miss 후 DB까지 계속 도달하는 문제**입니다.

예:

- 존재하지 않는 질문 ID를 계속 조회
- Redis에는 값이 없음
- 매번 DB까지 내려감

대응:

1. **null 캐싱**
   존재하지 않는 결과도 짧게 캐싱

2. **Bloom Filter**
   존재 가능성이 낮은 키를 미리 걸러냄

3. **입력 검증 강화**
   말이 안 되는 ID 형식이나 범위를 애플리케이션 앞단에서 차단

#### 캐시 눈사태(Cache Avalanche)

캐시 눈사태는 **많은 키가 비슷한 시점에 한꺼번에 만료되어 DB로 대량의 요청이 몰리는 현상**입니다.

예:

- 질문 상세 캐시 TTL을 모두 5분으로 동일하게 설정
- 같은 시각에 많은 키가 동시에 만료
- 그 순간 DB와 replica로 read burst 발생

대응:

1. **TTL jitter**
   키마다 만료 시간을 랜덤하게 분산

2. **핵심 키 영속 캐싱 + 수동 갱신**
   정말 중요한 hot key는 TTL 없이 관리하거나 별도 refresh 전략 사용

3. **다단계 캐시**
   local cache + Redis 조합으로 2차 방어

#### 캐시 스탬피드(Cache Stampede)

캐시 스탬피드는 **특정 hot key가 만료된 순간, 동시에 많은 요청이 들어와 모두가 DB 재생성을 시도하는 문제**입니다.

즉 눈사태가 "많은 키가 동시에 만료"라면, 스탬피드는 "하나 또는 소수의 hot key에 동시 요청이 몰리는 상황"에 더 가깝습니다.

예:

- `questionDetail::1`이 만료됨
- 동시에 수천 명이 같은 질문 상세를 요청
- 모두가 DB로 내려가며 폭주

대응:

1. **single-flight / per-key lock**
   한 요청만 DB를 읽고 나머지는 기다리게 함

2. **SWR**
   stale 값을 먼저 반환하고 백그라운드 재생성

3. **refresh ahead**
   만료 직전에 미리 갱신

4. **local cache 병행**
   Redis 앞단에서 추가 완충

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

#### Elasticsearch 비동기 색인으로 기대할 수 있는 점

이 구조는 MySQL을 검색 엔진처럼 직접 사용하는 대신, 검색용 read model을 별도 저장소에 두는 방식입니다.

1. **키워드 검색과 태그 검색에 더 적합함**
   질문 제목, 본문, 태그, 작성자 닉네임 같은 필드를 검색 친화적으로 색인할 수 있습니다.

2. **DB 조회 경로와 검색 경로를 분리할 수 있음**
   일반 목록/상세 조회는 MySQL과 Redis가 담당하고, 검색은 Elasticsearch가 담당하는 식으로 read path를 나눌 수 있습니다.

3. **연관도 기반 정렬이 가능함**
   Elasticsearch는 기본적으로 BM25 기반 relevance scoring을 사용하므로, 단순 일치 여부가 아니라 검색어와 얼마나 잘 맞는지를 기준으로 정렬할 수 있습니다.

4. **검색 결과용 모델을 따로 설계할 수 있음**
   이 프로젝트처럼 `QuestionSummary`와 별개로 `QuestionSearchDocument`, `QuestionSearchResponse`를 두면 검색 화면에 필요한 필드만 별도로 최적화할 수 있습니다.

#### 이 프로젝트의 검색 구조

이 프로젝트는 다음 구조를 사용합니다.

1. MySQL `questions`가 source of truth
2. 질문 생성/수정/삭제 시 `question.changed` 이벤트 발행
3. Kafka consumer가 이벤트를 읽어 ES 문서를 `upsert` 또는 `delete`
4. 검색 API는 MySQL이 아니라 Elasticsearch를 조회

즉 생성/수정과 검색 반영이 같은 트랜잭션이 아니므로, **검색은 eventual consistency를 전제로 한 read model**입니다.

#### 왜 upsert를 사용했는가

질문 변경 이벤트는 검색 관점에서는 "최신 상태로 맞춰라"가 핵심입니다.

- 생성이면 새 문서 생성
- 수정이면 같은 ID 문서 갱신
- 삭제만 별도 delete

그래서 검색 인덱스에서는 create/update를 세밀하게 나누기보다 `UPSERT + DELETE` 모델이 더 단순합니다.

#### 검색용 DTO와 목록 조회 DTO는 다름

이 프로젝트는 검색 결과와 목록 조회 결과를 같은 DTO로 처리하지 않습니다.

- 목록 조회: `QuestionSummary`
- 상세 조회: `QuestionDetailResponse`
- 검색 조회: `QuestionSearchResponse`

이유는 검색은 DB row를 그대로 보여주는 것이 아니라, **검색에 적합한 read model**을 별도로 갖는 편이 더 자연스럽기 때문입니다.

#### `_score`는 무엇인가

Elasticsearch 검색 결과의 `_score`는 **연관도 점수(relevance score)** 입니다.

- 검색어가 문서와 얼마나 잘 맞는지
- 어떤 필드에 매칭되었는지
- 흔한 단어인지, 중요한 단어인지

같은 요소를 바탕으로 계산됩니다.

Elasticsearch의 기본 텍스트 relevance는 일반적으로 **BM25** 기반이라고 이해하면 충분합니다.

#### 오타 검색과 퍼지 검색

현재 프로젝트는 기본적인 `multi_match + term filter` 수준이므로, 오타를 강하게 보정하는 fuzzy search까지는 구현하지 않았습니다.

하지만 Elasticsearch는 `fuzziness` 옵션을 통해 퍼지 검색을 지원할 수 있습니다.

퍼지 검색은 보통 **편집 거리(edit distance, Levenshtein distance)** 기반으로 동작합니다.

즉:

- 문자 하나 추가
- 문자 하나 삭제
- 문자 하나 치환

정도의 오타를 허용해 "비슷한 단어"를 찾는 방식입니다.

#### Elasticsearch 핵심 용어

처음 학습할 때는 아래 용어부터 잡으면 됩니다.

1. **index**
   검색용 문서들을 담는 공간. 이 프로젝트에서는 `questions`.

2. **document**
   인덱스 안의 JSON 한 건. 질문 하나가 document 하나가 됩니다.

3. **mapping**
   필드 구조와 타입 정의. RDB schema와 비슷하지만 더 유연합니다.

4. **shard**
   인덱스를 물리적으로 나눈 조각. 데이터 분산과 검색 분산의 기본 단위입니다.

5. **primary shard / replica shard**
   primary는 원본, replica는 복제본입니다. 단일 노드 환경에서는 replica를 둘 곳이 없어 `yellow` 상태가 흔합니다.

6. **score**
   검색 결과가 검색어와 얼마나 잘 맞는지 나타내는 점수입니다.

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

**코드 예시:**
```kotlin
override fun publishNotificationCreated(event: NotificationCreatedEvent) {
    kafkaTemplate.send(appProperties.topics.notificationCreated, event.userId.toString(), event)
}

fun onNotificationCreated(event: NotificationCreatedEvent) {
    consumeNotificationCreated(event)
}
```

#### 알림 비동기 분리로 기대할 수 있는 점

이 구조는 질문 등록, 답변 등록, 답변 채택 같은 핵심 요청에서 "알림 저장"을 직접 처리하지 않고 뒤로 넘기는 방식입니다.

1. **핵심 API 응답을 더 가볍게 유지할 수 있음**
   질문 저장이나 답변 저장은 핵심 트랜잭션만 끝내고 빠르게 응답하고, 알림은 Kafka consumer가 뒤에서 처리합니다.

2. **알림 로직을 별도 관심사로 분리할 수 있음**
   질문/답변 서비스는 "이벤트를 발행"하는 책임만 가지고, 실제 알림 저장은 consumer가 담당합니다.

3. **알림 처리 장애가 핵심 쓰기 로직을 바로 막지 않음**
   알림 저장이 잠깐 느리거나 실패하더라도, 질문/답변 생성 로직 자체와 분리되어 복구 전략을 독립적으로 가져갈 수 있습니다.

4. **이후 확장이 쉬움**
   같은 이벤트를 기반으로 DB 알림 저장 외에도 이메일, 푸시, Slack, 분석 시스템으로 확장할 수 있습니다.

#### 현재 알림 흐름

이 프로젝트의 알림 흐름은 아래와 같습니다.

1. 질문 생성 / 답변 생성 / 답변 채택
2. 서비스에서 `publishNotification(...)` 호출
3. `NotificationCreatedEvent`가 Kafka 토픽으로 발행
4. consumer가 이벤트를 받아 `notifications` 테이블에 저장
5. 프론트는 알림 조회 API를 다시 호출해 새 알림을 확인

즉 현재 구조는 **실시간 push 알림 시스템이 아니라, 비동기 저장된 알림을 조회 API로 확인하는 구조**입니다.

#### 읽음 처리와 프론트 반영

현재 구현 기준:

- `notifications.is_read`는 기본값 `false`
- 알림 조회는 가능
- 읽음 처리 API는 아직 없음
- 새 알림을 프론트에 실시간으로 push하는 SSE/WebSocket도 아직 없음

그래서 지금 프론트가 새 알림을 확인하는 방식은 보통 아래 둘 중 하나입니다.

1. **polling**
   일정 주기로 알림 API를 다시 호출

2. **화면 진입 시 재조회**
   사용자가 알림 패널을 열거나 프로필 화면에 들어갈 때 다시 조회

실시간 UX가 필요해지면 이후 단계로 다음을 붙일 수 있습니다.

- `PATCH /api/notifications/{id}/read` 같은 읽음 처리 API
- SSE 또는 WebSocket 기반 실시간 알림 push

#### 다만 같이 알아야 할 제약

1. **즉시 일관성이 아니라 eventual consistency**
   알림 이벤트가 발행된 직후 아주 짧은 시간 동안은 프로필 조회에서 아직 보이지 않을 수 있습니다.

2. **consumer 실패 시 재시도와 DLQ가 필요할 수 있음**
   운영 단계에서는 실패 메시지를 별도 DLQ로 보내 분석/재처리할 수 있게 하는 것이 일반적입니다.

3. **현재는 DB 알림 저장까지만 구현**
   프론트 실시간 push까지는 아직 포함하지 않았으므로, 새로고침 없는 실시간 UX는 polling 또는 추가 채널 구현이 필요합니다.

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
