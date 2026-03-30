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
/Users/amir.woo/workspace/dev-minsoo/backend-lab/topics/mongodb-cdc-kafka-pipeline/implementation
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
docker-compose up -d
```

포함된 서비스:

- MongoDB 7.0 Replica Set 모드
- Kafka 3.7 (KRaft)
- Kafka UI

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
