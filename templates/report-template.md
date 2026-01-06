# {주제명} - 성능 분석 리포트

**작성일:** YYYY-MM-DD
**작성자:** dev-minsoo
**실험 환경:** 로컬 개발 환경

---

## 🎯 실험 목적

### 검증하고자 하는 가설
예) "Fetch Join이 N+1 문제를 해결하고 성능을 X배 개선할 것이다"

### 측정 지표
- 쿼리 실행 횟수
- 응답 시간 (평균, 최소, 최대)
- 메모리 사용량
- CPU 사용률

## 🔧 실험 환경

### 소프트웨어 스펙
```yaml
Kotlin: 1.9.22
Spring Boot: 3.2.1
JVM: OpenJDK 17.0.9
Database: PostgreSQL 15.5
Connection Pool: HikariCP (default)
```

### 하드웨어 스펙
```yaml
CPU: Apple M1 Pro (8 cores)
Memory: 16GB
Storage: SSD
Database: Docker Container (2 CPU, 4GB RAM)
```

### 데이터셋

**테이블 구조:**
```sql
CREATE TABLE parent (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE child (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES parent(id),
    name VARCHAR(100)
);
```

**데이터 크기:**
- parent: 1,000 rows
- child: 10,000 rows (부모당 평균 10개)

## 📋 테스트 시나리오

### 시나리오 1: {시나리오명}

**테스트 케이스:**
```kotlin
@Test
fun `부모 100개 조회 시 N+1 문제 발생`() {
    // given
    val parents = createParentsWithChildren(100, 10)

    // when
    val result = parentRepository.findAll()
        .map { it.children.size }

    // then
    // 성능 측정
}
```

### 시나리오 2: ...

## 📊 측정 결과

### 1. 쿼리 실행 횟수

| 방법 | SELECT 쿼리 수 | 비고 |
|------|----------------|------|
| 일반 조회 (N+1) | 101회 | 부모 1회 + 자식 100회 |
| Fetch Join | 1회 | JOIN으로 한 번에 조회 |
| EntityGraph | 1회 | LEFT JOIN 사용 |
| Batch Size | 6회 | 부모 1회 + IN 절 5회 |

### 2. 응답 시간 (100회 실행 평균)

| 방법 | 평균 | 최소 | 최대 | 표준편차 | 개선율 |
|------|------|------|------|----------|--------|
| 일반 조회 | 342ms | 298ms | 451ms | 38ms | - |
| Fetch Join | 48ms | 42ms | 67ms | 8ms | **86%↑** |
| EntityGraph | 52ms | 45ms | 71ms | 9ms | 85%↑ |
| Batch Size | 89ms | 78ms | 112ms | 12ms | 74%↑ |

### 3. 메모리 사용량

| 방법 | Heap 사용량 | GC 빈도 |
|------|-------------|---------|
| 일반 조회 | 245MB | 8회 |
| Fetch Join | 198MB | 3회 |

### 4. 실행된 쿼리 로그

**일반 조회 (N+1 발생):**
```sql
-- 1. 부모 조회
SELECT p.id, p.name FROM parent p;

-- 2. 자식 조회 (100번 반복)
SELECT c.id, c.parent_id, c.name FROM child c WHERE c.parent_id = ?;
SELECT c.id, c.parent_id, c.name FROM child c WHERE c.parent_id = ?;
...
```

**Fetch Join:**
```sql
SELECT p.id, p.name, c.id, c.parent_id, c.name
FROM parent p
LEFT JOIN child c ON p.id = c.parent_id;
```

## 🔍 분석

### 주요 발견사항

1. **Fetch Join이 가장 빠른 성능**
   - 쿼리 1회로 모든 데이터 조회
   - 응답 시간 86% 개선
   - 메모리 사용량도 19% 감소

2. **Batch Size는 중간 성능**
   - N+1을 N/batch_size+1로 개선
   - 구현이 간단하지만 여전히 여러 쿼리 실행

3. **카테시안 곱 주의**
   - Fetch Join 사용 시 중복 데이터 발생 가능
   - `distinct()` 또는 `Set` 사용 필요

### 성능 차이 원인

**N+1 문제가 느린 이유:**
- 네트워크 왕복 시간 (RTT) 100배
- 커넥션 풀 경쟁
- 쿼리 파싱 오버헤드 100배

**Fetch Join이 빠른 이유:**
- 단일 쿼리로 네트워크 오버헤드 최소화
- DB 옵티마이저가 JOIN 최적화

## 💡 결론

### 권장사항

**다음 상황에서 각 방법 사용:**

| 상황 | 권장 방법 | 이유 |
|------|-----------|------|
| 1:N 관계, 모든 자식 필요 | Fetch Join | 최고 성능 |
| 1:N 관계, 일부만 필요 | 일반 조회 + 필터링 | 불필요한 데이터 방지 |
| 여러 컬렉션 Fetch | Batch Size | MultipleBagFetchException 회피 |
| 읽기 전용 | EntityGraph + DTO | 메모리 효율 |

### 주의사항

1. **Fetch Join 사용 시:**
   - 페이징 불가 (메모리에서 처리)
   - 중복 제거 필요 (`distinct`)

2. **Batch Size 사용 시:**
   - `hibernate.default_batch_fetch_size` 설정 필요
   - 적절한 사이즈 선택 (보통 10~100)

3. **EntityGraph 사용 시:**
   - `attributePaths`에 정확한 경로 지정
   - Lazy Loading 전략과 함께 고려

## 🚀 다음 단계

- [ ] 다양한 데이터 크기로 재실험 (1만, 10만건)
- [ ] 실제 프로덕션 환경 테스트
- [ ] QueryDSL을 활용한 동적 Fetch Join 구현
- [ ] DTO Projection 성능 비교

## 📎 부록

### 전체 테스트 코드
- [PerformanceTest.kt](implementation/src/test/kotlin/.../PerformanceTest.kt)

### 측정 도구
- JMH (Java Microbenchmark Harness)
- Spring Boot Actuator
- Hibernate Statistics

### 참고 자료
- [Hibernate 공식 문서 - Fetching](링크)
- [Vlad Mihalcea - N+1 문제](링크)
