# Codex 작업 가이드

> 이 문서는 Codex가 새로운 주제를 생성할 때 참고하는 가이드입니다.

## 📋 새 주제 생성 시 체크리스트

사용자가 "XXX 주제 시작해줘" 라고 요청하면:

### 1단계: 구조 생성
```bash
topics/{주제명}/
├── README.md                  # 템플릿 기반 작성
├── implementation/            # Spring Boot 프로젝트
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── .gitignore
│   ├── docker-compose.yml
│   ├── README.md             # 실행 가이드
│   └── src/
│       ├── main/kotlin/com/lab/{topic}/
│       │   ├── {Topic}Application.kt
│       │   ├── domain/
│       │   ├── repository/
│       │   ├── service/
│       │   └── config/
│       └── test/kotlin/com/lab/{topic}/
│           ├── integration/
│           └── performance/
├── report.md                 # 템플릿 복사
└── troubleshooting.md        # 템플릿 복사
```

### 2단계: TodoWrite 사용
작업 시작 전 TodoWrite로 계획 세우기:
```
1. {주제명} 디렉토리 구조 생성
2. 템플릿 파일 복사
3. Spring Boot 프로젝트 설정
4. 문제 재현용 엔티티/코드 구현
5. 해결 방법들 구현
6. 테스트 코드 작성
7. README.md 문서 작성
```

### 3단계: 참고 자료
- `PROJECT_STRUCTURE.md`: 전체 구조 이해
- `topics/jpa-n-plus-one/`: 레퍼런스 구현
- `templates/`: 문서 템플릿

## 🛠️ 기술 스택 (고정)

### 필수 의존성
```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}
```

### 주제별 추가 의존성

**QueryDSL 필요 시:**
```kotlin
kotlin("kapt") version "1.9.22"

dependencies {
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
}
```

**Redis 필요 시:**
```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("org.redisson:redisson-spring-boot-starter:3.25.0")
```

**Kafka 필요 시:**
```kotlin
implementation("org.springframework.kafka:spring-kafka")
testImplementation("org.springframework.kafka:spring-kafka-test")
```

## 📝 README.md 작성 가이드

`templates/topic-readme-template.md` 기반으로 작성하되:

### 필수 섹션
1. **제목 및 한 줄 요약**
2. **📌 개요**: 문제가 무엇이고 왜 중요한지
3. **🔍 문제 정의**:
   - 언제 발생하는가?
   - 왜 문제인가?
   - 실제 사례 (코드 예시)
4. **💡 발생 원인**: 기술적 원인 상세 설명
5. **🛠️ 해결 방법**: 각 방법마다
   - 설명
   - 장점/단점
   - 언제 사용하는가
   - 코드 예시 (Kotlin)
6. **📊 해결 방법 비교**: 비교표 (성능, 복잡도, 유지보수성 등)
7. **🚀 구현 체크리스트**
8. **🔬 테스트 실행**: 실행 방법
9. **📖 참고 자료**: 링크 3개 이상
10. **🔗 관련 주제**

### 코드 예시 작성 규칙
- 모든 코드는 **Kotlin**으로 작성
- 실제 동작하는 코드만 포함
- 주석으로 핵심 설명 추가

## 🧪 테스트 코드 작성 가이드

### 통합 테스트 (integration/)
```kotlin
@SpringBootTest
@Transactional
class {Topic}Test {

    @Test
    @DisplayName("문제 발생 케이스")
    fun `should demonstrate the problem`() {
        // given
        // when
        // then
    }

    @Test
    @DisplayName("해결 방법 1")
    fun `should solve with method 1`() {
        // given
        // when
        // then
    }
}
```

### 성능 테스트 (performance/)
```kotlin
@SpringBootTest
class {Topic}PerformanceTest {

    companion object {
        const val DATA_SIZE = 100
    }

    @Test
    @DisplayName("성능 비교")
    fun `performance comparison`() {
        // 각 방법의 실행 시간 측정
        // 개선율 계산 및 출력
    }
}
```

## 📦 Docker Compose 설정

주제에 따라 필요한 인프라 포함:

```yaml
version: '3.8'

services:
  postgres:  # 기본 포함
    image: postgres:15
    environment:
      POSTGRES_DB: {topic_name}
      POSTGRES_USER: {topic_name}
      POSTGRES_PASSWORD: {topic_name}
    ports:
      - "5432:5432"

  redis:  # Redis 주제일 때
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:  # Kafka 주제일 때
    image: confluentinc/cp-kafka:latest
    # ... 설정
```

## 🎯 작업 순서 (중요!)

1. **TodoWrite로 계획 작성** ← 필수!
2. 디렉토리 구조 생성
3. 템플릿 파일 복사
4. build.gradle.kts 작성 (의존성 포함)
5. settings.gradle.kts 작성
6. .gitignore 복사
7. application.yml 작성
8. Docker Compose 작성
9. 메인 Application 클래스 작성
10. 도메인 엔티티 작성
11. 리포지토리 작성 (여러 해결 방법 포함)
12. 서비스 작성
13. 설정 클래스 작성 (필요시)
14. 통합 테스트 작성
15. 성능 테스트 작성
16. implementation/README.md 작성 (실행 가이드)
17. topics/{주제명}/README.md 작성 (개념 정리)
18. TodoWrite로 완료 표시

## 📐 코딩 컨벤션

## 📝 Git 커밋 규칙

- 커밋 메시지는 **반드시 한국어로 작성**
- 가능하면 `타입(주제): 요약` 형식 사용
- 한 커밋에는 하나의 의도만 담기도록 분리

### 커밋 메시지 예시

```text
feat(redis): 레디스 종합편 주제 구조 추가
feat(redis): 캐시와 락 실습 코드 구현
docs(redis): 스터디 발표 가이드 문서 추가
fix(redis): 미사용 postgres 설정 제거
```

### 패키지 구조
```
com.lab.{topic}/
├── {Topic}Application.kt        # PascalCase
├── domain/                       # 엔티티
│   ├── {Entity1}.kt
│   └── {Entity2}.kt
├── repository/                   # 리포지토리
│   ├── {Entity1}Repository.kt
│   └── {Entity1}QueryDslRepository.kt  # QueryDSL 사용 시
├── service/                      # 서비스
│   └── {Entity1}Service.kt
└── config/                       # 설정
    └── QueryDslConfig.kt
```

### 네이밍 규칙
- **클래스**: PascalCase (Author, Book)
- **함수**: camelCase (findAllWithFetchJoin)
- **상수**: UPPER_SNAKE_CASE (DATA_SIZE)
- **패키지**: lowercase (domain, repository)

### 엔티티 작성 규칙
```kotlin
@Entity
@Table(name = "table_name")
class EntityName(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    // 연관관계는 마지막에
    @OneToMany(mappedBy = "parent")
    val children: MutableList<Child> = mutableListOf()
)
```

## 🚨 주의사항

### 반드시 확인할 것
1. ✅ QueryDSL 사용 시 kapt 설정 포함
2. ✅ allOpen, noArg 플러그인 설정 (JPA)
3. ✅ H2 + PostgreSQL 둘 다 지원
4. ✅ 테스트용 application.yml 별도 작성
5. ✅ .gitignore에 `**/generated/` 포함
6. ✅ Gradle wrapper properties 생성

### 피해야 할 것
1. ❌ findAllBasic() 같은 메서드에 @Query 없이 정의 (PropertyReferenceException 발생)
2. ❌ 테스트에서 @Transactional 없이 Lazy Loading 접근
3. ❌ Docker Compose 포트 충돌 (5432, 6379 등)
4. ❌ 빌드 결과물(build/, .gradle/) Git에 포함

## 📊 성능 비교표 템플릿

README.md에 항상 포함:

```markdown
| 구분 | 방법1 | 방법2 | 방법3 |
|------|-------|-------|-------|
| **쿼리 수** | N개 | 1개 | 2개 |
| **성능** | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **복잡도** | 낮음 | 중간 | 높음 |
| **페이징** | ✅ | ❌ | ✅ |
| **유지보수성** | 높음 | 보통 | 낮음 |
| **러닝커브** | 낮음 | 중간 | 높음 |
```

## 🔍 레퍼런스 구현

새 주제를 만들 때 참고할 수 있는 레퍼런스:

- `topics/jpa-n-plus-one/`: 완성된 구조
  - README.md 작성 방법
  - 테스트 코드 구조
  - 여러 해결 방법 구현 패턴
  - 성능 비교 테스트 방법

## 💬 사용자 소통

### 작업 시작 시
```
"{주제명} 주제를 시작하겠습니다.
다음 내용을 구현합니다:
- 문제 상황 재현
- 해결 방법 X가지 (방법1, 방법2, ...)
- 통합 테스트 및 성능 비교
- 상세한 문서화"
```

### 작업 완료 시
```
"완료했습니다!

구현된 내용:
1. 프로젝트 구조 ✅
2. X가지 해결 방법 ✅
3. 테스트 코드 ✅
4. 문서화 ✅

실행 방법:
cd topics/{주제명}/implementation
./gradlew test
"
```

## 🎓 주제별 특이사항

### JPA 관련 주제
- QueryDSL 포함
- Fetch Join, EntityGraph, Batch Size 등 비교
- 쿼리 로그 상세 설정

### 동시성 제어 주제
- 멀티스레드 테스트 필수
- 동시성 이슈 재현 코드
- 각 방법의 격리 수준 명시

### 성능 최적화 주제
- 성능 측정 도구 (measureTimeMillis)
- 대량 데이터 테스트 (최소 100개 이상)
- 개선율 계산 및 표시

### Redis/Kafka 주제
- Docker Compose 필수
- 외부 의존성 연결 테스트
- application-test.yml에 testcontainers 설정 고려

---

## ✅ 최종 체크리스트

새 주제 완성 시 확인:

- [ ] 디렉토리 구조가 PROJECT_STRUCTURE.md와 일치
- [ ] README.md에 모든 필수 섹션 포함
- [ ] 최소 3가지 이상의 해결 방법 구현
- [ ] 통합 테스트 및 성능 테스트 작성
- [ ] 모든 테스트 PASSED
- [ ] implementation/README.md 실행 가이드 작성
- [ ] .gitignore 적절히 설정
- [ ] Docker Compose 동작 확인
- [ ] 코드에 적절한 주석
- [ ] 비교표 포함
- [ ] 참고 자료 링크 3개 이상

---

**이 가이드를 따라 일관성 있는 고품질 학습 자료를 생성합니다!** 🚀
