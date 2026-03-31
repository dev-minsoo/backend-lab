# MongoDB CDC + Kafka 증분 처리 파이프라인 - 실행 가이드

## 🎯 목표

이 프로젝트는 실제 운영 파이프라인을 완전히 복제하기보다, 다음 질문에 답할 수 있도록 학습용 시나리오를 제공합니다.

- CDC 유실은 어떤 구간에서 발생할 수 있고 어떻게 검증하는가?
- Kafka 메시지 유실은 언제 발생하는가?
- 순서 보장은 어디까지 가능한가?
- MongoDB 모델링에서 Embed / Reference를 어떻게 선택하는가?
- Kotlin Scope Function은 왜 사용하는가?

## 🚀 실행 방법

### 1. 프로젝트 열기

IntelliJ IDEA에서 아래 경로를 엽니다.

```bash
/Users/q/projects/playground/backend-lab/topics/mongodb-cdc-kafka-pipeline/implementation
```

### 2. JDK 설정

- Java 17
- Gradle JVM도 Java 17로 설정

### 3. 테스트 실행

```bash
./gradlew test
```

특정 테스트만 실행:

```bash
./gradlew test --tests MongoDbCdcKafkaPipelineTest
./gradlew test --tests MongoDbCdcKafkaPipelinePerformanceTest
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

## 🐳 로컬 인프라 실행

MongoDB와 Kafka UI를 함께 띄우려면:

```bash
docker compose up -d
```

포함된 서비스:

- MongoDB 7.0 Replica Set 모드
- Kafka 3.7 (KRaft)
- Kafka UI

MongoDB Change Stream 계열 기능을 연습하려면 replica set 초기화가 한 번 필요합니다.

```bash
docker exec mongodb-cdc-lab mongosh --eval 'rs.initiate({_id:"rs0",members:[{_id:0,host:"localhost:27017"}]})'
```

이미 초기화된 뒤라면 아래 명령으로 상태만 확인하면 됩니다.

```bash
docker exec mongodb-cdc-lab mongosh --quiet --eval 'rs.status().ok'
```

## 🧪 바로 해보는 실습

### 1. 애플리케이션 실행

이미 8080 포트를 쓰는 앱이 있다면 8082 같은 다른 포트로 실행합니다.

```bash
./gradlew bootRun
./gradlew bootRun --args='--server.port=8082'
```

### 2. 주문 변경 이벤트 발행

아래 요청은 "MongoDB 변경 이벤트가 Kafka로 전달된다"는 상황을 단순화해 재현합니다.

```bash
curl -X POST http://localhost:8080/lab/orders/events \
  -H 'Content-Type: application/json' \
  -d '{
    "aggregateId": "order-100",
    "version": 1,
    "status": "CREATED",
    "customerId": "user-1",
    "totalAmount": 15000
  }'
```

응답으로 발행된 CDC 이벤트를 확인할 수 있습니다.

### 3. MongoDB projection 조회

Kafka consumer가 이벤트를 읽어 projection을 MongoDB에 반영하면 아래 조회가 성공합니다.

```bash
curl http://localhost:8080/lab/orders/order-100/projection
```

예상 응답:

```json
{
  "aggregateId": "order-100",
  "latestAppliedVersion": 1,
  "status": "CREATED",
  "customerId": "user-1",
  "totalAmount": 15000
}
```

### 4. 같은 aggregate의 다음 버전 발행

```bash
curl -X POST http://localhost:8080/lab/orders/events \
  -H 'Content-Type: application/json' \
  -d '{
    "aggregateId": "order-100",
    "version": 2,
    "status": "PAID",
    "customerId": "user-1",
    "totalAmount": 15000
  }'
```

다시 projection을 조회하면 `latestAppliedVersion`과 `status`가 갱신됩니다.

### 5. 이전 버전 재전송으로 멱등성 확인

이미 반영된 오래된 버전을 다시 보내도 projection은 되돌아가지 않습니다.

```bash
curl -X POST http://localhost:8080/lab/orders/events \
  -H 'Content-Type: application/json' \
  -d '{
    "aggregateId": "order-100",
    "version": 1,
    "status": "CREATED",
    "customerId": "user-1",
    "totalAmount": 15000
  }'
```

이후 projection을 다시 조회하면 최신 버전 2 상태가 유지됩니다.

### 6. Kafka UI에서 topic / partition 확인

- [http://localhost:8081](http://localhost:8081) 접속
- `order-events` 토픽 확인
- partition 수가 3개인지 확인
- 같은 `aggregateId`를 반복 발행하며 어느 partition으로 가는지 관찰

## 🔬 학습 시나리오

### 시나리오 1: CDC 유실 검증

`CdcVerificationService`는 다음 관점으로 정합성을 확인합니다.

- resume token 연속성
- source / topic / sink 건수 비교
- aggregate별 최신 버전 반영 여부

### 시나리오 2: Kafka 순서 보장

`OrderingGuaranteeService`는 다음 원칙을 코드로 보여줍니다.

- 전역 순서가 아니라 aggregate 단위 순서를 보장
- 동일 aggregate id를 동일 partition key로 사용
- partition 수보다 큰 consumer concurrency는 안전하지 않음

### 시나리오 3: Kafka 유실 조건 분석

`KafkaDeliveryRiskService`는 Producer / Broker / Consumer 구간별로 유실 가능 조건을 설명합니다.

### 시나리오 4: MongoDB 모델링 선택

`MongoModelingDecisionService`는 조회 패턴과 cardinality를 기준으로 Embed / Reference 중 하나를 선택하는 예제를 제공합니다.

### 시나리오 5: Kotlin Scope Function 사용 의도

`ScopeFunctionGuideService`는 `let`, `run`, `apply`, `also`, `with`를 왜 쓰는지와 남용 시 주의점을 정리합니다.

## 📚 추천 학습 순서

1. [../README.md](../README.md) 읽기
2. 통합 테스트 실행
3. 성능 테스트 로그 확인
4. [../report.md](../report.md)로 관찰 결과 정리
5. [../troubleshooting.md](../troubleshooting.md)에 시행착오 추가
