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

---

## Issue #4: MySQL replica가 기동했지만 복제가 실제로 동작하지 않음

**발생일:** 2026-03-23
**심각도:** 🔴 High
**소요 시간:** 1시간 30분

### 📌 상황

`docker compose up -d` 이후 primary에는 Flyway가 생성한 테이블이 보였지만 replica는 `qna_primary` 스키마만 있고 테이블이 비어 있었습니다.

### 🐛 증상

- replica에서 `SHOW TABLES;` 결과가 비어 있음
- `SHOW REPLICA STATUS;` 결과에서 `Replica_IO_Running = No`, `Replica_SQL_Running = No`
- 초기 로그에 `--super-read-only` 상태에서 replication 설정 명령을 수행하지 못했다는 에러가 나타남

### 🔍 원인 분석

문제는 두 단계로 나뉘었습니다.

1. 처음에는 `bitnami/mysql` 이미지를 사용하려 했지만 사용 가능한 태그와 설정이 맞지 않았습니다.
2. 공식 `mysql:8.0.36`로 전환한 뒤에는 replica를 기동 시점부터 `super_read_only=ON`으로 둬서, init 스크립트 안의 `CHANGE REPLICATION SOURCE TO`, `START REPLICA` 명령이 실행되지 못했습니다.

즉, "읽기 전용 replica"를 너무 일찍 강제한 것이 핵심 원인이었습니다.

### ✅ 해결 방법

#### 1. 공식 MySQL 이미지로 전환

- `mysql-primary`, `mysql-replica` 모두 `mysql:8.0.36` 사용
- primary/replica 각각 별도 `my.cnf`와 init 스크립트로 replication 구성

#### 2. replica read-only 적용 순서 변경

replica 설정 파일에서는 `read_only`, `super_read_only`를 제거하고, replication 연결이 완료된 뒤 init 스크립트에서 활성화하도록 변경했습니다.

```bash
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-primary',
  SOURCE_PORT=3306,
  SOURCE_USER='repl_user',
  SOURCE_PASSWORD='repl_password',
  SOURCE_AUTO_POSITION=1,
  GET_SOURCE_PUBLIC_KEY=1;

START REPLICA;
SET GLOBAL read_only = ON;
SET GLOBAL super_read_only = ON;
```

#### 3. 점검 순서 정리

실행 순서는 아래처럼 가져가야 혼선이 적었습니다.

1. `docker compose up -d`
2. primary / replica 모두 정상 기동 확인
3. replica에서 `SHOW REPLICA STATUS;` 확인
4. 그 다음 Spring Boot 기동

### ✅ 확인 쿼리

replica에서 가장 먼저 확인한 값:

```sql
SHOW REPLICA STATUS;
```

중요 컬럼:

- `Replica_IO_Running`
- `Replica_SQL_Running`
- `Last_IO_Error`
- `Last_SQL_Error`

정상 상태는 둘 다 `Yes`입니다.

### 💭 회고

- replication 환경은 앱보다 저장소 상태를 먼저 검증해야 함
- replica가 비어 있을 때는 DataGrip 트리보다 `SHOW REPLICA STATUS`가 더 신뢰할 수 있음
- `read_only`는 "replica 보호"를 위해 필요하지만, 초기 bootstrap 이후에 적용해야 함
