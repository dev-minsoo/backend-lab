# MongoDB CDC + Kafka 증분 처리 파이프라인 - 성능 분석 리포트

**작성일:** 2026-03-30
**작성자:** dev-minsoo
**실험 환경:** 로컬 개발 환경

---

## 🎯 실험 목적

### 검증하고자 하는 가설

1. aggregate key 기준 partitioning은 순서 보장 설명을 단순화한다.
2. CDC 유실 검증은 source / topic / sink 3구간 비교가 가장 설명력이 높다.
3. MongoDB 모델링은 성능보다 조회 패턴 적합성이 우선이다.

### 측정 지표

- resume token 연속성
- source / topic / sink 건수 차이
- aggregate별 최신 버전 반영 여부
- partition assignment 계산 시간
- 모델링 판단 일관성

## 🔧 실험 환경

### 소프트웨어 스펙

```yaml
Kotlin: 1.9.22
Spring Boot: 3.2.1
JVM: OpenJDK 17
MongoDB: 7.0
Kafka: 3.7
```

### 하드웨어 스펙

```yaml
CPU: Apple Silicon
Memory: 16GB
Storage: SSD
```

### 데이터셋

- CDC 이벤트: 10,000건
- Aggregate 수: 250개
- Partition 수: 3개

## 📋 테스트 시나리오

### 시나리오 1: resume token 누락 검출

- 입력: 일부 token이 빠진 CDC 이벤트 목록
- 기대 결과: 누락 token 식별

### 시나리오 2: 정합성 대사

- 입력: source / topic / sink 건수 스냅샷
- 기대 결과: 어느 구간에서 차이가 났는지 문장으로 설명

### 시나리오 3: aggregate key partition 계산

- 입력: 10,000건 이벤트
- 기대 결과: partition assignment 계산 시간이 충분히 낮고 반복 실행 가능

### 시나리오 4: MongoDB 모델링 선택 일관성

- 입력: embed에 적합한 프로필 5,000개, reference에 적합한 프로필 5,000개
- 기대 결과: 규칙 기반 판단 결과가 일관됨

## 📊 측정 결과

### 1. 정합성 검증 포인트

| 검증 축 | 확인 항목 | 의미 |
|---------|-----------|------|
| Source | MongoDB 변경 건수 | CDC 원천 데이터 |
| Topic | Kafka 적재 건수 | 브로커까지 전달 성공 여부 |
| Sink | Projection 반영 건수 | 소비 후 최종 반영 여부 |

### 2. 순서 보장 설계 결과

| 항목 | 설계 방식 | 결과 |
|------|-----------|------|
| Message Key | `aggregateId` | 동일 aggregate는 동일 partition |
| Ordering Scope | Partition 단위 | 전역 순서 아님 |
| Consumer Concurrency | Partition 수 이하 | 설명 가능한 순서 보장 |
| Retry Strategy | 멱등 처리 전제 | 중복 허용, 재처리 가능 |

### 3. 성능 테스트 관찰

| 항목 | 관찰 |
|------|------|
| Partition Assignment | 10,000건 기준 매우 짧은 시간 내 계산 |
| Modeling Decision | 단순 규칙 평가라 비용이 거의 없음 |
| 병목 후보 | 실제 운영 병목은 계산보다 외부 I/O와 재처리 경로 |

## 🔍 분석

### 주요 발견사항

1. CDC 검증은 실시간 소비 로그보다 대사 관점이 더 중요했다.
2. Kafka 순서는 "보장 여부"보다 "보장 범위"를 말하는 것이 핵심이었다.
3. 메시지 유실은 Kafka 자체보다 offset commit / 예외 처리 미흡에서 더 자주 발생한다.
4. MongoDB 모델링은 성능 마이크로벤치보다 조회 패턴 설명이 더 중요하다.

### 실무 적용 시 체크 포인트

- resume token 저장 위치와 재기동 전략
- producer `acks`, retry, idempotence 설정
- consumer commit 시점과 DLQ 운영 방식
- aggregate version 기반 멱등 처리
- MongoDB 문서 크기 증가 가능성

## 💡 결론

### 권장사항

| 상황 | 권장 접근 |
|------|-----------|
| CDC 유실 검증 | source/topic/sink 3단계 대사 |
| 순서 보장 | aggregate key 기준 partitioning |
| 중복 소비 | 멱등 처리 + 재처리 허용 |
| 실패 메시지 | DLQ와 운영 절차 함께 설계 |
| MongoDB 모델링 | read / write pattern 우선 판단 |

### 다음 단계

- [ ] 실제 MongoDB Change Stream 또는 Debezium 연결
- [ ] DLQ 토픽과 재처리 consumer 추가
- [ ] version 기반 멱등 projection 저장소 구현
- [ ] Kafka lag, DLQ 적재량 메트릭 추가
