# 온라인 QnA 서비스 실습 순서 체크리스트

이 문서는 [apidog-openapi.yaml](/Users/q/projects/playground/backend-lab/topics/online-qna-service/apidog-openapi.yaml) 기준으로 어떤 API를 어떤 순서로 호출하고, 각 단계에서 저장소/인프라 계층을 어떻게 관찰하면 되는지 정리한 실습 가이드입니다.

## 실습 목표

아래 5가지를 직접 확인합니다.

1. MySQL primary / replica 분리
2. Redis 캐시 적재와 무효화
3. Kafka 비동기 이벤트 발행
4. Elasticsearch 색인과 검색
5. 질문/답변/채택/알림 흐름

## 사전 준비

### 1. 인프라 실행

```bash
cd topics/online-qna-service/implementation
docker compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 확인 도구

- ApiDog: API 호출
- DataGrip: MySQL primary / replica 확인
- Redis Insight: 캐시 확인
- 터미널: Kafka / Elasticsearch 확인

## 관찰 대상

### MySQL

- primary: `localhost:3306`
- replica: `localhost:3307`
- database: `qna_primary`

### Redis

- host: `localhost`
- port: `6379`

### Elasticsearch

```bash
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/questions/_search?pretty
```

### Kafka

토픽 목록:

```bash
docker exec -it online-qna-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

이벤트 확인:

```bash
docker exec -it online-qna-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic qna.question.changed \
  --from-beginning
```

```bash
docker exec -it online-qna-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic qna.notification.created \
  --from-beginning
```

## 실습 시나리오

## Step 1. 회원가입

### API

- `POST /api/auth/signup`

### 예시 요청

```json
{
  "email": "user1@example.com",
  "nickname": "user1",
  "password": "password1234"
}
```

두 번째 사용자도 같은 방식으로 하나 더 만듭니다.

```json
{
  "email": "user2@example.com",
  "nickname": "user2",
  "password": "password1234"
}
```

### 확인 포인트

- 응답에서 `accessToken` 획득
- primary `users` 테이블에 사용자 2명 생성
- replica에도 잠시 후 동일 데이터 반영

### 핵심 코드

- [AuthController.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/controller/AuthController.kt)
- [AuthService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/AuthService.kt)
- [JwtTokenProvider.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/security/JwtTokenProvider.kt)

### 확인 SQL

```sql
SELECT id, email, nickname, role FROM users;
```

## Step 2. 질문 작성

### API

- `POST /api/questions`

### 인증

- `user1` 토큰 사용

### 예시 요청

```json
{
  "title": "Redis와 read replica는 역할이 어떻게 다른가요?",
  "content": "read-heavy 서비스에서 둘 다 왜 필요한지 궁금합니다.",
  "tags": ["redis", "mysql", "system-design"]
}
```

### 확인 포인트

- primary의 `questions`, `tags`, `question_tags` 저장
- Kafka `qna.question.changed` 이벤트 발행
- Kafka `qna.notification.created` 이벤트 발행
- Elasticsearch `questions` 인덱스에 문서 생성
- Redis에는 아직 상세 캐시가 없을 수 있음

### 핵심 코드

- [QuestionController.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/controller/QuestionController.kt)
- [QuestionService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/QuestionService.kt)
- [TagService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/TagService.kt)
- [KafkaEventPublisher.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/KafkaEventPublisher.kt)
- [EventConsumers.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/EventConsumers.kt)

### 확인 SQL

```sql
SELECT id, title, author_id, answer_count, accepted_answer_id FROM questions;
SELECT * FROM tags;
SELECT * FROM question_tags;
SELECT * FROM notifications;
```

### Elasticsearch 확인

```bash
curl http://localhost:9200/questions/_search?pretty
```

## Step 3. 질문 목록 조회

### API

- `GET /api/questions?page=0&size=20`

### 확인 포인트

- 첫 조회 후 Redis `questionList` 캐시 생성
- 결과에 태그, 작성자, 투표 수, answerCount 포함

### 핵심 코드

- [QuestionService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/QuestionService.kt)
- [QuestionRepository.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/repository/QuestionRepository.kt)
- [CacheConfig.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/config/CacheConfig.kt)
- [DataSourceConfig.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/config/DataSourceConfig.kt)

### Redis 확인

Redis Insight에서 다음 키 패턴 확인:

- `questionList`

## Step 4. 질문 상세 조회

### API

- `GET /api/questions/{questionId}`

예: `GET /api/questions/1`

### 확인 포인트

- 첫 조회 후 Redis `questionDetail` 캐시 생성
- 반복 조회 시 같은 키 재사용
- 상세 응답에 answers, votes, acceptedAnswerId 포함

### 핵심 코드

- [QuestionService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/QuestionService.kt)
- [AnswerRepository.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/repository/AnswerRepository.kt)
- [VoteService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/VoteService.kt)
- [Mappers.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/support/Mappers.kt)

### Redis 확인

Redis Insight에서 다음 키 패턴 확인:

- `questionDetail`

## Step 5. 검색 확인

### API

- `GET /api/search/questions?keyword=Redis&tags=redis`

### 확인 포인트

- DB 목록 조회와 별도로 ES 검색 결과 반환
- 태그 필터와 키워드 검색이 동시에 동작

### 핵심 코드

- [SearchController.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/controller/SearchController.kt)
- [SearchService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/SearchService.kt)
- [ElasticSearchIndexService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/ElasticSearchIndexService.kt)
- [EventConsumers.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/EventConsumers.kt)

### Elasticsearch 확인

```bash
curl -X GET http://localhost:9200/questions/_search \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "bool": {
        "must": [
          {
            "multi_match": {
              "query": "Redis",
              "fields": ["title", "content", "authorNickname", "tags"]
            }
          }
        ],
        "filter": [
          {
            "term": {
              "tags.keyword": "redis"
            }
          }
        ]
      }
    }
  }'
```

## Step 6. 답변 작성

### API

- `POST /api/questions/{questionId}/answers`

### 인증

- `user2` 토큰 사용

### 예시 요청

```json
{
  "content": "Redis는 hot key를 줄이고, replica는 일반 read 부하를 분산합니다."
}
```

### 확인 포인트

- primary `answers` 테이블에 저장
- 질문의 `answer_count` 증가
- 질문 작성자에게 알림 생성
- `questionDetail`, `questionList` 캐시 무효화
- Kafka 질문 변경 이벤트 재발행
- Elasticsearch 문서 갱신

### 핵심 코드

- [AnswerService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/AnswerService.kt)
- [QuestionService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/QuestionService.kt)
- [EventConsumers.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/EventConsumers.kt)

### 확인 SQL

```sql
SELECT id, question_id, author_id, accepted, deleted FROM answers;
SELECT id, title, answer_count, accepted_answer_id FROM questions;
SELECT id, user_id, message, reference_id, reference_type FROM notifications ORDER BY id DESC;
```

## Step 7. 답변 채택

### API

- `POST /api/answers/{answerId}/accept`

### 인증

- `user1` 토큰 사용

### 예시 요청

```json
{
  "questionId": 1
}
```

### 확인 포인트

- `answers.accepted = true`
- `questions.accepted_answer_id` 반영
- 답변 작성자에게 알림 생성
- Kafka 알림 이벤트 발행
- Elasticsearch 문서 갱신

### 핵심 코드

- [AnswerController.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/controller/AnswerController.kt)
- [AnswerService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/AnswerService.kt)
- [EventPublisher.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/EventPublisher.kt)

### 확인 SQL

```sql
SELECT id, question_id, accepted FROM answers;
SELECT id, accepted_answer_id FROM questions;
SELECT id, user_id, message FROM notifications ORDER BY id DESC;
```

## Step 8. 좋아요 / 싫어요

### API

- `POST /api/questions/{questionId}/vote`
- `POST /api/answers/{answerId}/vote`

### 예시 요청

```json
{
  "type": "LIKE"
}
```

또는

```json
{
  "type": "DISLIKE"
}
```

### 확인 포인트

- `votes` 테이블 적재
- 동일 사용자가 다시 누르면 같은 row가 갱신됨
- 상세 조회 응답의 `votes.likes`, `votes.dislikes` 반영

### 핵심 코드

- [VoteService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/VoteService.kt)
- [VoteRepository.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/repository/VoteRepository.kt)
- [QuestionController.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/controller/QuestionController.kt)
- [AnswerController.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/controller/AnswerController.kt)

### 확인 SQL

```sql
SELECT id, user_id, target_type, target_id, type FROM votes;
```

## Step 9. 신고 기능

### API

- `POST /api/questions/{questionId}/report`
- `POST /api/answers/{answerId}/report`

### 예시 요청

```json
{
  "reason": "광고성 게시물입니다."
}
```

### 확인 포인트

- `reports` 테이블 적재

### 핵심 코드

- [Report.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/domain/Report.kt)
- [QuestionService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/QuestionService.kt)
- [AnswerService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/AnswerService.kt)

### 확인 SQL

```sql
SELECT id, reporter_id, target_type, target_id, reason FROM reports;
```

## Step 10. 내 프로필 조회

### API

- `GET /api/profiles/me`

### 인증

- 각 사용자 토큰으로 각각 호출

### 확인 포인트

- 내 질문 목록
- 내 답변 목록
- 내 알림 목록

### 핵심 코드

- [ProfileController.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/controller/ProfileController.kt)
- [ProfileService.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/service/ProfileService.kt)
- [NotificationRepository.kt](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/src/main/kotlin/com/lab/onlineqna/repository/NotificationRepository.kt)

## 장애 / 이상 상황 점검 체크리스트

### replica가 비어 보일 때

먼저 DataGrip 트리보다 SQL로 확인:

```sql
SHOW REPLICA STATUS;
SHOW TABLES;
```

확인 컬럼:

- `Replica_IO_Running`
- `Replica_SQL_Running`
- `Last_IO_Error`
- `Last_SQL_Error`

### 핵심 코드

- [docker-compose.yml](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/docker-compose.yml)
- [my.cnf](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/mysql/primary/conf.d/my.cnf)
- [my.cnf](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/mysql/replica/conf.d/my.cnf)
- [01-setup-replica.sh](/Users/q/projects/playground/backend-lab/topics/online-qna-service/implementation/mysql/replica/init/01-setup-replica.sh)

### ES 문서가 안 보일 때

- `qna.question.changed` 이벤트가 발행됐는지 Kafka 확인
- `questions` 인덱스 존재 여부 확인

### Redis 캐시가 안 보일 때

- 해당 조회 API를 실제로 두 번 이상 호출했는지 확인
- 수정/답변 등록 후에는 캐시가 비워질 수 있음

## 추천 실습 순서 요약

1. 회원가입 2명
2. 질문 작성
3. 질문 목록 조회
4. 질문 상세 조회 2회
5. ES 검색 확인
6. 답변 작성
7. 답변 채택
8. 질문/답변 좋아요
9. 신고 기능
10. 프로필 조회

이 순서를 따르면 "쓰기 -> 이벤트 -> 검색/알림 -> 캐시 -> replica 반영" 흐름을 한 번에 볼 수 있습니다.
