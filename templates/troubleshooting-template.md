# {주제명} - 문제 해결 과정

---

## Issue #1: {문제 제목}

**발생일:** 2025-01-03
**심각도:** 🔴 High / 🟡 Medium / 🟢 Low
**소요 시간:** 2시간

### 📌 상황

어떤 작업을 하다가 어떤 상황에서 문제가 발생했는지 설명

예) "100개의 부모 엔티티를 조회할 때 응답이 5초 이상 걸리는 문제 발견"

### 🐛 증상

**관찰된 현상:**
- 응답 시간: 5.2초 (기대: 100ms 이하)
- 데이터베이스 쿼리 101개 실행됨
- 로그에 동일한 SELECT 쿼리가 반복됨

**에러 로그/스택 트레이스:**
```
2025-01-03 14:23:45.123 DEBUG --- [nio-8080-exec-1] org.hibernate.SQL
: select parent0_.id as id1_0_, parent0_.name as name2_0_ from parent parent0_

2025-01-03 14:23:45.234 DEBUG --- [nio-8080-exec-1] org.hibernate.SQL
: select children0_.parent_id as parent_i3_1_0_, children0_.id as id1_1_0_
  from child children0_ where children0_.parent_id=?

... (100번 반복)
```

### 🔍 원인 분석

**처음 의심했던 부분:**
1. 데이터베이스 인덱스 문제인가? → 인덱스는 정상
2. 쿼리가 복잡해서? → 쿼리 자체는 단순함

**디버깅 과정:**
```kotlin
// 1. Hibernate Statistics 활성화
spring.jpa.properties.hibernate.generate_statistics=true

// 2. 로그 레벨 조정
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.stat=DEBUG
```

**실제 원인:**
- JPA의 **N+1 문제** 발견
- `Parent` 엔티티 조회 시 `children` 필드가 `LAZY` 로딩
- 각 부모마다 자식 조회 쿼리가 별도로 실행됨
- Fetch 전략을 설정하지 않아 기본 동작으로 N+1 발생

### ✅ 해결 방법

**시도했던 방법들:**

#### 시도 1: EAGER로 변경 ❌
```kotlin
@Entity
class Parent(
    @OneToMany(fetch = FetchType.EAGER)  // 이렇게 하면?
    val children: List<Child> = emptyList()
)
```
- 결과: 모든 조회에서 항상 자식을 가져와서 불필요한 성능 저하
- 문제: 필요 없을 때도 데이터를 가져옴

#### 시도 2: Fetch Join 적용 ✅
```kotlin
interface ParentRepository : JpaRepository<Parent, Long> {

    @Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children")
    fun findAllWithChildren(): List<Parent>
}
```
- 결과: **쿼리 1개로 감소, 응답 시간 48ms**
- 장점: 필요할 때만 사용 가능

#### 최종 해결책: EntityGraph 사용 ✅
```kotlin
interface ParentRepository : JpaRepository<Parent, Long> {

    @EntityGraph(attributePaths = ["children"])
    override fun findAll(): List<Parent>
}
```
- Fetch Join보다 간결한 코드
- 유연한 사용 가능

### 📊 해결 전후 비교

| 지표 | 해결 전 | 해결 후 | 개선율 |
|------|---------|---------|--------|
| 응답 시간 | 5,234ms | 48ms | **99.1%↑** |
| 쿼리 수 | 101개 | 1개 | 99%↓ |
| DB CPU | 85% | 12% | 86%↓ |

### 💭 회고

**배운 점:**
1. JPA 기본 Fetch 전략(LAZY)의 동작 원리 이해
2. 성능 문제는 먼저 측정하고 분석해야 함
3. Hibernate Statistics가 문제 파악에 매우 유용

**아쉬운 점:**
- 처음부터 로깅을 켜고 확인했으면 더 빨리 해결

**다음에는:**
- 새로운 엔티티 추가 시 항상 Fetch 전략 고려
- 통합 테스트에 쿼리 개수 assert 추가
- 성능 테스트 자동화

### 🔗 관련 자료

- [Hibernate N+1 문제 정리](../README.md#n1-문제)
- [성능 측정 리포트](report.md)

---

## Issue #2: {다른 문제 제목}

...

---

## Issue #3: {또 다른 문제}

...
