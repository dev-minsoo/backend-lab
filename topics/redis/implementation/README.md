# Redis 종합편 v1 - 실행 가이드

## 🚀 IntelliJ IDEA에서 실행하기

### 1. 프로젝트 열기

- 경로: `/Users/q/projects/playground/backend-lab/topics/redis/implementation`
- `build.gradle.kts`를 기준으로 열면 됩니다.

### 2. JDK 설정 확인

- Java 17 사용

### 3. Redis 실행

```bash
docker-compose up -d redis
```

애플리케이션 실행 시 기본적으로 `localhost:6379` Redis를 사용합니다.

### 4. 테스트 실행

```bash
./gradlew test
```

테스트는 embedded Redis를 사용하므로 별도 로컬 Redis 없이도 실행됩니다.

### 5. 데모 화면 실행

Redis 컨테이너를 띄운 뒤 애플리케이션을 실행합니다.

```bash
docker-compose up -d redis
./gradlew bootRun
```

브라우저에서 아래 주소를 열면 됩니다.

```text
http://localhost:8080/demo/redis
```

## 📚 포함된 실습

- `basics`: Redis 자료구조와 TTL
- `cache`: Spring Cache + Redis
- `pubsub`: Redis Pub/Sub
- `lock`: 단순 락과 Redisson 락
- `leaderboard`: Sorted Set 기반 랭킹
- `ratelimit`: 고정 윈도우 카운터

## 🔧 주의사항

- 애플리케이션 데모는 로컬 Redis가 필요합니다.
- Pub/Sub는 영속 메시징이 아니므로 재처리 예제는 포함하지 않았습니다.
- 분산 락은 학습용 예제이며, 운영 환경에서는 락 만료 시간과 장애 상황을 더 엄격히 설계해야 합니다.
