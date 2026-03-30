# 온라인 QnA 서비스 실행 가이드

## 실행 대상

- MySQL primary / replica
- Redis
- Elasticsearch
- Kafka
- Spring Boot 애플리케이션

## 1. 인프라 실행

```bash
cd topics/online-qna-service/implementation
docker compose up -d
```

기본 포트:

- MySQL Primary: `3306`
- MySQL Replica: `3307`
- Redis: `6379`
- Elasticsearch: `9200`
- Kafka: `9092`
- Kafka UI: `8081`

호환성 기준 버전:

- Redis: `7.2-alpine`
- Elasticsearch: `8.10.4`
- Kafka Image: `confluentinc/cp-kafka:7.6.10`
- Kafka UI Image: `provectuslabs/kafka-ui:latest`

이 조합은 "최신"보다 현재 프로젝트의 `Spring Boot 3.2.1` 의존성과의 호환성을 우선한 값입니다.

## 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

## 3. 테스트 실행

```bash
./gradlew test
```

## 4. 주요 API

### 회원가입

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user1@example.com",
    "nickname":"user1",
    "password":"password1234"
  }'
```

### 질문 생성

```bash
curl -X POST http://localhost:8080/api/questions \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Redis 캐시와 read replica를 같이 써야 하나요?",
    "content":"read-heavy 서비스에서 둘의 역할이 어떻게 다른지 궁금합니다.",
    "tags":["redis","mysql","system-design"]
  }'
```

### 질문 목록 조회

```bash
curl "http://localhost:8080/api/questions?page=0&size=20"
```

### 답변 등록

```bash
curl -X POST http://localhost:8080/api/questions/1/answers \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "content":"Redis는 hot read를 줄이고, replica는 일반 read를 분산합니다."
  }'
```

### 태그 검색

```bash
curl "http://localhost:8080/api/search/questions?keyword=Redis&tags=redis&tags=mysql"
```

### 내 프로필 조회

```bash
curl http://localhost:8080/api/profiles/me \
  -H "Authorization: Bearer {TOKEN}"
```

## 5. 인프라 UI

Kafka UI:

- URL: `http://localhost:8081`
- 확인 가능 항목:
  - Topics
  - Messages
  - Consumer Groups
  - Partitions

Kafka UI에서 보면 좋은 포인트:

- `Topics > qna.question.changed`
  - 질문 생성/수정/삭제 이벤트가 쌓이는지 확인
- `Topics > qna.notification.created`
  - 알림 생성 이벤트 payload 확인
- `Consumer Groups`
  - 각 consumer group의 lag 확인
  - lag가 `0`이면 현재까지 들어온 메시지를 따라잡은 상태
  - lag가 증가한 채 유지되면 consumer가 처리 속도를 못 따라가거나 오류로 멈춘 상태일 수 있음
- `Messages`
  - 질문 생성 후 `questionId`, `changeType`
  - 알림 생성 후 `userId`, `referenceId`, `referenceType`
  - 값이 기대한 payload인지 바로 확인 가능

lag를 볼 때 해석 기준:

- 짧게 `1 ~ 2` 정도 튀었다가 다시 `0`으로 내려가면 정상적인 소비 과정일 수 있음
- 계속 `0`이면 producer는 보내고 consumer는 잘 처리하고 있는 상태
- 계속 누적되면 consumer 병목, 예외 재시도, deserialization 문제를 의심
- 메시지는 있는데 lag가 줄지 않으면 앱 로그와 함께 봐야 함

실습용 관찰 순서:

1. 질문 생성 또는 답변 작성
2. Kafka UI `Topics`에서 메시지 payload 확인
3. `Consumer Groups`에서 lag가 일시적으로 생겼다가 다시 `0`으로 돌아오는지 확인
4. 이후 MySQL notifications 또는 Elasticsearch 반영 결과 확인

참고:

- 로컬 애플리케이션은 Kafka에 `localhost:9092`로 접속합니다.
- Kafka UI는 같은 Docker 네트워크 안에서 `kafka:29092` 내부 listener를 사용합니다.

## 구현 포인트

### 1. Read / Write 분리

- `@Transactional(readOnly = true)` 경로는 replica
- 쓰기 트랜잭션은 primary
- `@UsePrimaryDataSource`가 붙은 읽기 전용 조회는 primary로 강제 라우팅
- 적용 대상: `login`, `getQuestion`, `getQuestionsByAuthor`, `getAnswersByAuthor`, `getProfile`

### 2. Redis 캐시

- 질문 상세: `questionDetail`
- 질문 목록: `questionList`

### 3. Kafka 비동기 이벤트

- `qna.question.changed`
- `qna.notification.created`

### 4. Elasticsearch 검색

- 질문 제목, 본문, 태그, 작성자 닉네임 색인
- 태그 필터 + 키워드 검색 지원

## 주의사항

- replica는 asynchronous replication 특성상 약간의 지연이 있을 수 있습니다.
- 회원가입 직후 로그인, 질문/답변 작성 직후 상세/프로필 조회처럼 read-after-write가 중요한 경로는 stale read가 발생할 수 있어 primary 우회가 필요합니다.
- 검색과 알림은 eventual consistency 기반입니다.
- 로컬 compose는 학습용이며 운영용 HA 설정과는 다릅니다.
