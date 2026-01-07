# JPA N+1 문제

> JPA에서 연관 관계 조회 시 발생하는 대표적인 성능 문제와 다양한 해결 방법 비교

## 📌 개요

N+1 문제는 JPA를 사용할 때 가장 흔하게 마주치는 성능 문제입니다. 처음 실행한 쿼리(1) 결과에 따라 추가로 N개의 쿼리가 실행되어 총 N+1개의 쿼리가 발생하는 현상입니다. 특히 연관 관계가 설정된 엔티티를 조회할 때 발생하며, 데이터가 많을수록 성능 저하가 심각해집니다.

## 🔍 문제 정의

### 언제 발생하는가?

- **OneToMany, ManyToOne 등 연관 관계**가 설정된 엔티티를 조회할 때
- **지연 로딩(Lazy Loading)**으로 설정된 연관 엔티티에 접근할 때
- **즉시 로딩(Eager Loading)**을 사용하더라도 JPQL을 사용하면 발생
- 리스트 조회 후 **반복문에서 연관 엔티티에 접근**할 때

### 왜 문제인가?

1. **성능 저하**: 100개의 데이터를 조회하면 최악의 경우 101개의 쿼리 실행
2. **데이터베이스 부하**: 불필요한 쿼리로 인한 DB 커넥션 낭비
3. **응답 시간 증가**: 쿼리가 많아질수록 API 응답 시간 급격히 증가
4. **확장성 문제**: 데이터가 늘어날수록 문제가 기하급수적으로 악화

### 실제 사례

```kotlin
// Author 엔티티
@Entity
class Author(
    @Id @GeneratedValue
    val id: Long? = null,
    val name: String,

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    val books: MutableList<Book> = mutableListOf()
)

// N+1 문제 발생 코드
fun findAllAuthors() {
    val authors = authorRepository.findAll()  // 1번의 쿼리

    authors.forEach { author ->
        println("${author.name}의 책: ${author.books.size}권")  // N번의 추가 쿼리!
    }
}
```

**실행되는 쿼리:**
```sql
-- 1. Author 조회 (1번)
SELECT * FROM authors;

-- 2. 각 Author의 books 조회 (N번)
SELECT * FROM books WHERE author_id = 1;
SELECT * FROM books WHERE author_id = 2;
SELECT * FROM books WHERE author_id = 3;
-- ... (Author 수만큼 반복)
```

## 💡 발생 원인

### 1. JPA의 기본 전략
- JPA는 연관 엔티티를 **기본적으로 지연 로딩(Lazy Loading)** 으로 처리
- 연관 엔티티는 **프록시 객체**로 초기화되고, 실제 접근 시점에 쿼리 실행

### 2. JPQL의 동작 방식
- JPQL은 **SQL로 직접 변환**되어 실행
- 연관 관계의 Fetch 전략을 무시하고 **쿼리 그대로 실행**
- 이후 연관 엔티티에 접근할 때 추가 쿼리 발생

### 3. 즉시 로딩의 한계
- `FetchType.EAGER`를 사용해도 JPQL에서는 N+1 발생 가능
- 불필요한 데이터까지 항상 조회하여 오히려 성능 저하

## 🛠️ 해결 방법

### 방법 1: Fetch Join (JPQL)

**설명:**
JPQL에서 `JOIN FETCH` 키워드를 사용하여 연관 엔티티를 한 번에 조회합니다.

**장점:**
- 한 번의 쿼리로 모든 데이터 조회
- 가장 직관적이고 명확한 방법
- 성능이 우수

**단점:**
- 페이징과 함께 사용 시 메모리에서 처리 (경고 발생)
- 둘 이상의 컬렉션 Fetch Join 불가
- 쿼리가 복잡해질 수 있음

**언제 사용하는가:**
- 연관된 데이터를 **항상 함께 사용**하는 경우
- 페이징이 필요 없거나 데이터가 많지 않은 경우
- 1:N 관계에서 N의 데이터를 확실히 사용하는 경우

**코드 예시:**
```kotlin
// Repository
@Query("SELECT DISTINCT a FROM Author a JOIN FETCH a.books")
fun findAllWithFetchJoin(): List<Author>

// QueryDSL
fun findAllWithFetchJoin(): List<Author> {
    return queryFactory
        .selectFrom(author)
        .distinct()
        .leftJoin(author.books, book).fetchJoin()
        .fetch()
}
```

### 방법 2: EntityGraph

**설명:**
Spring Data JPA의 `@EntityGraph` 애너테이션으로 Fetch Join을 선언적으로 처리합니다.

**장점:**
- 애너테이션 기반으로 간편하게 사용
- JPQL 작성 없이 Fetch Join 효과
- 코드가 간결함

**단점:**
- Fetch Join과 동일한 제약사항
- 복잡한 조건에서는 JPQL이 더 유연
- 동적 쿼리 작성이 어려움

**언제 사용하는가:**
- 간단한 연관 관계 조회
- JPQL 없이 빠르게 적용하고 싶을 때
- Spring Data JPA의 메서드 네이밍 쿼리를 사용하는 경우

**코드 예시:**
```kotlin
@EntityGraph(attributePaths = ["books"])
@Query("SELECT a FROM Author a")
fun findAllWithEntityGraph(): List<Author>

// 또는 메서드 네이밍 쿼리와 함께
@EntityGraph(attributePaths = ["books"])
fun findByName(name: String): List<Author>
```

### 방법 3: Batch Size

**설명:**
`@BatchSize` 또는 `hibernate.default_batch_fetch_size` 설정으로 지연 로딩 시 IN 절로 한 번에 조회합니다.

**장점:**
- 기존 코드 수정 없이 설정만으로 적용 가능
- N+1을 완전히 해결하지는 못하지만 크게 완화 (N+1 → 1+1)
- 페이징과 함께 사용 가능
- 여러 컬렉션에도 적용 가능

**단점:**
- Fetch Join보다는 쿼리가 1개 더 발생 (총 2개)
- IN 절의 개수 제한을 고려해야 함
- 지연 로딩이므로 트랜잭션 범위 주의

**언제 사용하는가:**
- 페이징이 필요한 경우
- 여러 컬렉션을 조회해야 하는 경우
- 전역적으로 N+1 문제를 완화하고 싶을 때
- 연관 데이터를 항상 사용하지는 않는 경우

**코드 예시:**
```kotlin
// Entity에 직접 설정
@Entity
class Author(
    @OneToMany(mappedBy = "author")
    @BatchSize(size = 100)
    val books: MutableList<Book> = mutableListOf()
)

// application.yml에 전역 설정 (권장)
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

**실행되는 쿼리:**
```sql
-- 1. Author 조회
SELECT * FROM authors;

-- 2. Books를 IN 절로 한 번에 조회 (100개씩)
SELECT * FROM books WHERE author_id IN (1, 2, 3, ..., 100);
```

### 방법 4: QueryDSL 별도 쿼리

**설명:**
연관 엔티티를 별도 쿼리로 조회한 후 애플리케이션 레벨에서 조합합니다.

**장점:**
- Fetch Join의 제약 없이 자유로운 쿼리 작성
- 각 쿼리를 독립적으로 최적화 가능
- 페이징 사용 가능

**단점:**
- 쿼리가 최소 2번 이상 실행
- 애플리케이션 메모리에서 조합 필요
- 코드가 복잡해질 수 있음

**언제 사용하는가:**
- Fetch Join 사용이 어려운 복잡한 조건
- 각 엔티티를 독립적으로 최적화해야 할 때
- 동적 쿼리가 많은 경우

**코드 예시:**
```kotlin
fun findAllWithSeparateQuery(): List<Author> {
    // 1. Author 먼저 조회
    val authors = queryFactory
        .selectFrom(author)
        .fetch()

    val authorIds = authors.map { it.id }

    // 2. Books를 IN 쿼리로 한 번에 조회
    queryFactory
        .selectFrom(book)
        .where(book.author.id.`in`(authorIds))
        .fetch()

    return authors  // JPA가 자동으로 연관관계 매핑
}
```

## 📊 해결 방법 비교

| 구분 | Fetch Join | EntityGraph | Batch Size | QueryDSL 별도 쿼리 |
|------|------------|-------------|------------|-------------------|
| **쿼리 수** | 1개 | 1개 | 2개 | 2개 이상 |
| **성능** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **복잡도** | 중간 | 낮음 | 매우 낮음 | 높음 |
| **페이징** | ❌ (메모리) | ❌ (메모리) | ✅ | ✅ |
| **다중 컬렉션** | ❌ | ❌ | ✅ | ✅ |
| **유지보수성** | 높음 | 높음 | 매우 높음 | 보통 |
| **러닝커브** | 낮음 | 매우 낮음 | 매우 낮음 | 중간 |
| **적용 범위** | 메서드별 | 메서드별 | 전역/엔티티별 | 메서드별 |

### 권장 사항

1. **일반적인 경우**: Fetch Join 또는 EntityGraph
2. **페이징이 필요한 경우**: Batch Size 또는 별도 쿼리
3. **전역 최적화**: application.yml에 `default_batch_fetch_size` 설정
4. **복잡한 조건**: QueryDSL 별도 쿼리
5. **여러 컬렉션**: Batch Size

## 🚀 구현 체크리스트

- [x] 문제 상황 재현
- [x] Fetch Join 구현 및 테스트
- [x] EntityGraph 구현 및 테스트
- [x] Batch Size 구현 및 테스트
- [x] QueryDSL Fetch Join 구현 및 테스트
- [x] QueryDSL 별도 쿼리 구현 및 테스트
- [x] 성능 벤치마크 테스트
- [ ] report.md 작성
- [ ] troubleshooting.md 작성

## 🔬 테스트 실행

```bash
cd implementation

# 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests NPlusOneTest
./gradlew test --tests NPlusOnePerformanceTest
```

## 📖 참고 자료

- [Hibernate Query Plan Cache](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#fetching)
- [Spring Data JPA - EntityGraph](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph)
- [Vlad Mihalcea - N+1 Query Problem](https://vladmihalcea.com/n-plus-1-query-problem/)
- [QueryDSL Reference](http://querydsl.com/static/querydsl/latest/reference/html/)
- [자바 ORM 표준 JPA 프로그래밍 - 김영한](https://www.inflearn.com/course/ORM-JPA-Basic)

## 🔗 관련 주제

- [영속성 컨텍스트](../jpa-persistence-context/README.md)
- [쿼리 최적화](../query-optimization/README.md)
- [배치 처리](../batch-processing/README.md)
