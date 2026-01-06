# Backend Lab - 프로젝트 구조 개발 가이드

## 🎯 프로젝트 목적

백엔드 개발 중 마주하는 다양한 문제들(JPA N+1, 동시성 제어, 성능 최적화 등)을 Kotlin과 Spring Boot로 **학습 → 구현 → 테스트 → 문서화**하는 체계적인 학습 저장소

### 핵심 목표
- 실제 문제 상황 재현 및 해결
- 여러 해결 방법 비교 및 성능 측정
- 체계적인 문서화로 포트폴리오 활용
- 기술 면접 및 블로그 포스트 자료로 활용

## 🛠️ 기술 스택

- **Language:** Kotlin 1.9.x
- **Framework:** Spring Boot 3.2.x
- **Build Tool:** Gradle (Kotlin DSL)
- **Testing:** JUnit 5, Kotest, MockK
- **Database:** PostgreSQL, MySQL
- **Infrastructure:** Docker, Redis, Kafka

## 📦 GitHub Repository 설정

### Repository Name
`backend-lab`

### Description (추천)
```
🔬 A structured collection of backend engineering topics with hands-on implementations, performance reports, and troubleshooting guides | Kotlin + Spring Boot
```

### Topics (태그)
`kotlin` `spring-boot` `jpa` `concurrency` `performance` `backend` `learning` `playground` `lab`

## 🏗️ 전체 디렉토리 구조

```
backend-lab/
├── README.md                           # 전체 저장소 소개 & 인덱스
├── .gitignore
├── PROJECT_STRUCTURE.md                # 이 문서 (구조 설명)
├── templates/                          # 문서 템플릿 모음
│   ├── topic-readme-template.md
│   ├── report-template.md
│   └── troubleshooting-template.md
└── topics/                             # 주제별 학습 디렉토리
    ├── jpa-n-plus-one/                 # 예시 주제 1
    │   ├── README.md
    │   ├── implementation/
    │   │   ├── build.gradle.kts
    │   │   ├── settings.gradle.kts
    │   │   ├── docker-compose.yml
    │   │   └── src/
    │   │       ├── main/kotlin/
    │   │       └── test/kotlin/
    │   ├── report.md
    │   └── troubleshooting.md
    │
    ├── redis-distributed-lock/         # 예시 주제 2
    │   ├── README.md
    │   ├── implementation/
    │   ├── report.md
    │   └── troubleshooting.md
    │
    └── ... (추가 주제들)
```

## 📁 디렉토리 및 파일 역할

### `topics/{주제명}/` - 주제별 독립 학습 단위

각 주제는 4가지 핵심 요소로 구성:

#### 1. **README.md** - 개념 & 학습 내용
- 문제 정의 및 발생 원인
- 해결 방법들 (이론 정리)
- 각 방법의 장단점 비교
- 사용 시기 및 가이드라인
- 참고 자료 링크

#### 2. **implementation/** - 실제 구현 코드
- 독립 실행 가능한 Spring Boot 프로젝트
- 여러 해결 방법 구현 (패키지 분리)
- 테스트 코드로 검증
- Docker Compose로 인프라 구성

**기본 구조:**
```
implementation/
├── build.gradle.kts
├── settings.gradle.kts
├── docker-compose.yml
├── src/
│   ├── main/kotlin/com/lab/{topic}/
│   │   ├── domain/
│   │   ├── repository/
│   │   ├── service/
│   │   └── config/
│   └── test/kotlin/com/lab/{topic}/
│       ├── integration/
│       └── performance/
└── README.md           # 실행 방법 가이드
```

#### 3. **report.md** - 실험 결과 & 성능 분석
- 실험 목적 및 가설
- 실험 환경 (버전, 스펙, 데이터 크기)
- 테스트 시나리오
- 측정 결과 (표, 그래프, 쿼리 로그)
- 분석 및 인사이트
- 결론 및 권장사항

#### 4. **troubleshooting.md** - 문제 해결 과정
- 실제 겪은 문제들 기록
- 디버깅 과정 및 시행착오
- 해결 방법 및 코드
- 회고 및 배운 점

## 📝 문서 템플릿

템플릿 파일들은 `templates/` 디렉토리에 위치합니다:

- **topic-readme-template.md** - 주제별 README 템플릿
- **report-template.md** - 성능 분석 리포트 템플릿
- **troubleshooting-template.md** - 문제 해결 과정 템플릿

## 🎯 추천 주제 목록

### Category 1: JPA & Database

- **jpa-n-plus-one** - N+1 문제와 해결 방법들
- **jpa-persistence-context** - 영속성 컨텍스트 관리
- **jpa-entity-mapping** - 엔티티 매핑 전략
- **transaction-isolation** - 트랜잭션 격리 수준
- **database-indexing** - 인덱스 최적화
- **query-optimization** - 쿼리 튜닝
- **database-deadlock** - 데드락 분석 및 해결

### Category 2: 동시성 제어

- **redis-distributed-lock** - Redis 분산 락 (Redisson vs Lettuce)
- **kafka-exactly-once** - Kafka Exactly-Once 처리
- **optimistic-locking** - JPA 낙관적 락
- **pessimistic-locking** - JPA 비관적 락
- **coroutine-concurrency** - 코루틴 동시성 제어
- **virtual-threads** - Virtual Threads 활용

### Category 3: 성능 최적화

- **caching-strategies** - 캐싱 전략 (Local/Redis)
- **batch-processing** - 대용량 배치 처리
- **async-processing** - 비동기 처리
- **connection-pool** - 커넥션 풀 최적화
- **api-response-time** - API 응답 시간 개선

### Category 4: 아키텍처 패턴

- **event-driven-architecture** - 이벤트 기반 아키텍처
- **cqrs-pattern** - CQRS 패턴
- **saga-pattern** - Saga 패턴
- **hexagonal-architecture** - 헥사고날 아키텍처

### Category 5: Spring Boot 심화

- **custom-starter** - Custom Starter 개발
- **spring-aop** - Spring AOP 활용
- **spring-security-custom** - Spring Security 커스터마이징
- **actuator-custom-metrics** - Actuator 커스텀 메트릭
- **spring-batch** - Spring Batch 활용

## 🚀 작업 프로세스

### 1단계: 새로운 주제 시작

```bash
# 1. 주제 디렉토리 생성
mkdir -p topics/{주제명}/implementation

# 2. 템플릿 복사
cp templates/topic-readme-template.md topics/{주제명}/README.md
cp templates/report-template.md topics/{주제명}/report.md
cp templates/troubleshooting-template.md topics/{주제명}/troubleshooting.md

# 3. implementation 디렉토리로 이동
cd topics/{주제명}/implementation
```

### 2단계: Spring Boot 프로젝트 생성

**build.gradle.kts 기본 템플릿:**
```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.lab"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
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
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

**docker-compose.yml 기본 템플릿:**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: playground-postgres
    environment:
      POSTGRES_DB: playground
      POSTGRES_USER: playground
      POSTGRES_PASSWORD: playground
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: playground-redis
    ports:
      - "6379:6379"

volumes:
  postgres-data:
```

### 3단계: 학습 및 구현

1. **README.md 작성** - 개념 정리 및 이론 학습
2. **코드 구현** - 여러 해결 방법 구현
3. **테스트 작성** - 각 방법 검증
4. **성능 측정** - 벤치마크 테스트

### 4단계: 문서화

1. **report.md** - 성능 측정 결과 정리
2. **troubleshooting.md** - 겪은 문제 기록
3. **루트 README.md** - 인덱스 업데이트

## 💡 효율적인 학습 팁

### 학습 전략
1. **작은 단위로 시작** - 한 번에 하나의 주제에 집중
2. **테스트 주도** - 테스트 코드로 검증하며 학습
3. **측정 중심** - 추측하지 말고 측정하기
4. **즉시 문서화** - 배운 내용을 바로 기록

### 면접 활용
- "어려웠던 기술적 문제는?" → `troubleshooting.md` 참고
- "성능 개선 경험은?" → `report.md` 참고
- "기술 선택 근거는?" → `README.md` 비교표 참고

### 블로그 포스트 전환
- `README.md` → 개념 설명 포스트
- `report.md` → 실험/벤치마크 포스트
- `troubleshooting.md` → 트러블슈팅 포스트

## ✅ 초기 설정 체크리스트

프로젝트 시작 시 다음을 확인하세요:

```markdown
- [x] templates/ 디렉토리 생성
- [x] topic-readme-template.md 추가
- [x] report-template.md 추가
- [x] troubleshooting-template.md 추가
- [x] topics/ 디렉토리 생성
- [x] README.md 작성
- [x] PROJECT_STRUCTURE.md 추가
- [ ] .gitignore 설정
- [ ] Git 저장소 초기화
- [ ] GitHub 저장소 생성 및 연결
- [ ] 첫 번째 주제 선정
- [ ] 첫 번째 주제 디렉토리 생성
```

## 🎓 참고 자료

### 공식 문서
- [Kotlin 공식 문서](https://kotlinlang.org/docs/home.html)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

### 추천 도서
- "자바 ORM 표준 JPA 프로그래밍" - 김영한
- "Kotlin in Action"
- "Real MySQL 8.0"

### 블로그
- [Baeldung Kotlin](https://www.baeldung.com/kotlin/)
- [Vlad Mihalcea](https://vladmihalcea.com/)

---

**이 문서와 함께 프로젝트를 시작하세요!**
