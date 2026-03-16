# Redis 종합편 v1

> Redis의 핵심 이론부터 캐시, Pub/Sub, 분산 락, 랭킹, Rate Limit 실습까지 한 번에 다루는 학습 주제

## 📌 개요

Redis는 단순한 캐시 서버가 아니라, 다양한 자료구조와 빠른 응답 성능을 바탕으로 캐시, 세션, 실시간 메시징, 분산 락, 카운터, 랭킹 시스템까지 폭넓게 활용되는 인메모리 데이터 저장소입니다.

이 주제는 Redis를 "왜 빠른가" 수준의 이론부터, Spring Boot 애플리케이션에서 실제로 어떻게 붙이고 어떤 용도로 쓰는지까지 한 번에 익히는 것을 목표로 합니다.

## 🔍 문제 정의

### 언제 Redis를 배우고 써야 하는가?

- 데이터베이스만으로는 응답 시간이 느릴 때
- 같은 데이터를 반복 조회해 DB 부하가 커질 때
- 실시간 알림이나 이벤트 전달이 필요할 때
- 여러 서버에서 공유하는 락이나 카운터가 필요할 때
- 랭킹, 조회수, 요청 제한처럼 빠른 집계가 필요할 때

### 왜 중요한가?

Redis는 백엔드 실무에서 다음 문제를 빠르게 해결해 줍니다.

1. 반복 조회를 줄여 응답 시간을 낮춘다.
2. 메모리 기반 자료구조로 빠른 연산을 제공한다.
3. 단순한 명령 조합만으로도 분산 시스템 문제를 많이 풀 수 있다.
4. Spring Boot와의 통합이 쉬워 실습 대비 학습 효과가 크다.

### 실제 사례

```kotlin
@Cacheable(cacheNames = ["products"], key = "#productId")
fun getProduct(productId: Long): ProductView {
    return productRepository.findById(productId)
        .orElseThrow()
        .toView()
}
```

위 코드는 가장 흔한 Redis 활용 예시입니다. 같은 상품을 여러 번 조회해도 DB 대신 Redis 캐시에서 응답할 수 있습니다.

## 💡 핵심 이론

### Redis는 왜 인메모리인가?

- 기본 저장 위치가 디스크가 아니라 메모리라서 접근 속도가 매우 빠릅니다.
- 디스크 기반 DB보다 랜덤 I/O 비용이 훨씬 낮습니다.
- 자료구조 연산 자체가 메모리 안에서 끝나는 경우가 많습니다.

### Redis는 왜 싱글 스레드인데 빠른가?

- 명령 처리가 짧고 단순합니다.
- 자료구조 구현이 최적화되어 있습니다.
- 컨텍스트 스위칭과 락 경합 비용이 작습니다.
- 네트워크 이벤트 루프 기반으로 많은 요청을 효율적으로 처리합니다.

주의할 점도 있습니다.

- 싱글 스레드라서 긴 명령이나 무거운 Lua 스크립트는 전체 지연을 키웁니다.
- 빠르다고 해서 항상 DB를 대체하는 것은 아닙니다.
- 캐시 정합성, 메모리 정책, 영속화 전략을 함께 고려해야 합니다.

## 🛠️ 실습 챕터

### 챕터 1: Basics

**설명:**
String, Hash, Set, List, Sorted Set, TTL 같은 Redis 기본 자료구조를 직접 다룹니다.

**실습 포인트:**
- 문자열 저장
- Hash 필드 조회
- Set/List 동작 확인
- Sorted Set으로 정렬된 결과 조회
- TTL 확인

### 챕터 2: Cache

**설명:**
Spring Cache와 Redis를 연결해 Cache Aside 패턴의 기본 형태를 실습합니다.

**장점:**
- 가장 실무적인 Redis 활용 방식
- DB 부하 감소 효과를 바로 확인 가능

**단점:**
- 캐시 무효화 전략이 필요
- 오래된 데이터(stale data) 가능성 존재

**언제 사용하는가:**
- 조회가 많고 변경이 비교적 적은 데이터
- 상품, 게시글, 설정 정보, 코드성 데이터

### 챕터 3: Pub/Sub

**설명:**
Redis 채널에 메시지를 발행하고 구독자가 수신하는 흐름을 실습합니다.

**장점:**
- 설정이 단순함
- 실시간 알림 개념을 이해하기 좋음

**단점:**
- durable queue가 아님
- 구독자가 없으면 메시지 유실 가능

**언제 사용하는가:**
- 경량 이벤트 알림
- 내부 실시간 시그널 전달

### 챕터 4: Distributed Lock

**설명:**
`SET NX PX` 기반 단순 락과 Redisson 기반 락을 비교합니다.

**장점:**
- 여러 인스턴스에서 공유하는 임계 구역 보호 가능
- 동시성 실습 효과가 큼

**단점:**
- 구현을 잘못하면 오히려 위험
- lease time, unlock 검증이 중요

**언제 사용하는가:**
- 재고 차감
- 중복 처리 방지
- 배치 중복 실행 방지

### 챕터 5: Leaderboard

**설명:**
Sorted Set으로 점수 저장, 상위 N명 조회, 개별 순위 조회를 실습합니다.

**언제 사용하는가:**
- 게임 랭킹
- 학습 점수판
- 인기 순위 집계

### 챕터 6: Rate Limit

**설명:**
고정 윈도우 카운터 방식으로 요청 수를 제한합니다.

**장점:**
- 구현이 가장 단순함
- Redis `INCR`와 TTL 조합만으로 구성 가능

**단점:**
- 윈도우 경계에서 burst 요청에 취약

**언제 사용하는가:**
- API 남용 방지
- 로그인 시도 제한
- 외부 연동 호출 보호

## 📊 해결 방법 비교

| 구분 | Cache | Pub/Sub | Lock | Leaderboard | Rate Limit |
|------|-------|---------|------|-------------|------------|
| **핵심 자료구조/명령** | String, TTL | Channel | SET NX PX, RLock | Sorted Set | INCR, EXPIRE |
| **주요 목적** | 조회 성능 개선 | 이벤트 전달 | 동시성 제어 | 순위 집계 | 요청 제한 |
| **성능** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **복잡도** | 낮음 | 낮음 | 중간 | 낮음 | 낮음 |
| **정합성 주의** | 높음 | 중간 | 매우 높음 | 낮음 | 중간 |
| **실무 활용도** | 매우 높음 | 중간 | 높음 | 높음 | 매우 높음 |

## 🚀 구현 체크리스트

- [x] Redis 기본 자료구조 실습
- [x] Cache Aside 형태의 캐시 예제 구현
- [x] Pub/Sub 발행 및 구독 예제 구현
- [x] 단순 락과 Redisson 락 구현
- [x] Sorted Set 랭킹 구현
- [x] 고정 윈도우 Rate Limit 구현
- [x] 통합 테스트 작성
- [x] 기본 성능 비교 테스트 작성
- [x] implementation/README.md 실행 가이드 작성
- [x] report.md 작성
- [x] troubleshooting.md 작성

## 🔬 테스트 실행

```bash
cd topics/redis/implementation
./gradlew test
```

## 📖 참고 자료

- [Redis Documentation](https://redis.io/docs/latest/)
- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/reference/)
- [Redisson Documentation](https://redisson.org/documentation/)
- [Redis Pub/Sub](https://redis.io/docs/latest/develop/interact/pubsub/)
- [Redis Distributed Locks](https://redis.io/docs/latest/develop/use/patterns/distributed-locks/)

## 🔗 관련 주제

- [JPA N+1 문제](../jpa-n-plus-one/README.md)
