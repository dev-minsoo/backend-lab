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

호환성 기준 버전:

- Redis: `7.2-alpine`
- Elasticsearch: `8.10.4`
- Kafka Image: `confluentinc/cp-kafka:7.6.10`

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

## 구현 포인트

### 1. Read / Write 분리

- `@Transactional(readOnly = true)` 경로는 replica
- 쓰기 트랜잭션은 primary

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
- 검색과 알림은 eventual consistency 기반입니다.
- 로컬 compose는 학습용이며 운영용 HA 설정과는 다릅니다.
