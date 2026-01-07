# JPA N+1 문제 - 실행 가이드

## 🚀 IntelliJ IDEA에서 실행하기

### 1. 프로젝트 열기

1. **IntelliJ IDEA 실행**

2. **프로젝트 열기**
   ```
   File → Open...
   ```
   경로: `/Users/q/projects/playground/backend-lab/topics/jpa-n-plus-one/implementation`

   또는 `build.gradle.kts` 파일을 선택

3. **"Open as Project" 선택**
   - 팝업이 뜨면 "Open as Project" 클릭

4. **Gradle 동기화 대기**
   - IntelliJ가 자동으로 Gradle wrapper를 생성
   - 의존성 다운로드 (우측 하단 진행 상황 확인)
   - 완료될 때까지 대기 (처음엔 몇 분 소요)

### 2. JDK 설정 확인

1. `File` → `Project Structure` (⌘; 또는 Ctrl+Alt+Shift+S)
2. `Project` 탭:
   - SDK: **Java 17** 선택
   - Language level: **17**

### 3. QueryDSL Q클래스 생성

**방법 1: Gradle 창에서 실행 (권장)**
1. 우측 `Gradle` 탭 클릭
2. `jpa-n-plus-one` → `Tasks` → `other` → `kaptKotlin` 더블클릭
3. 또는 `build` → `build` 실행하면 자동 생성됨

**방법 2: 터미널에서 실행**
```bash
./gradlew kaptKotlin
```

**생성 확인:**
- `build/generated/source/kapt/main/` 경로에 Q클래스 생성됨
- `QAuthor.kt`, `QBook.kt` 파일 확인

### 4. 테스트 실행

**방법 1: IntelliJ UI에서**
1. `src/test/kotlin` 폴더에서 테스트 클래스 열기
2. 클래스나 메서드 옆의 ▶️ 아이콘 클릭
3. `Run 'NPlusOneTest'` 선택

**방법 2: Gradle 창에서**
1. 우측 `Gradle` 탭
2. `Tasks` → `verification` → `test` 더블클릭

**방법 3: 터미널에서**
```bash
# 전체 테스트
./gradlew test

# 특정 테스트만
./gradlew test --tests NPlusOneTest
./gradlew test --tests NPlusOnePerformanceTest
```

### 5. 애플리케이션 실행

**Spring Boot 앱 실행:**
1. `src/main/kotlin/com/lab/nplusone/NPlusOneApplication.kt` 열기
2. `main` 함수 옆의 ▶️ 클릭
3. 또는 터미널에서:
   ```bash
   ./gradlew bootRun
   ```

## 📊 테스트 결과 확인

테스트를 실행하면 콘솔에서 다음을 확인할 수 있습니다:

### 1. N+1 문제 발생 케이스
```
=== N+1 문제 발생 케이스 ===
Hibernate: SELECT * FROM authors
Hibernate: SELECT * FROM books WHERE author_id = 1
Hibernate: SELECT * FROM books WHERE author_id = 2
Hibernate: SELECT * FROM books WHERE author_id = 3
총 4개의 쿼리가 실행됨 (1 + N)
```

### 2. Fetch Join으로 해결
```
=== Fetch Join으로 해결 ===
Hibernate: SELECT DISTINCT a.*, b.* FROM authors a LEFT JOIN books b ON a.id = b.author_id
총 1개의 쿼리가 실행됨
```

### 3. 성능 비교 결과
```
================================================================================
성능 비교 결과 요약
================================================================================
1. N+1 문제:          150ms
2. Fetch Join:        25ms (↑83% 개선)
3. EntityGraph:       26ms (↑82% 개선)
4. QueryDSL Fetch:    27ms (↑82% 개선)
5. QueryDSL 별도:     30ms (↑80% 개선)
6. Batch Size:        35ms (↑76% 개선)
================================================================================
```

## 🐘 PostgreSQL 사용하기 (선택사항)

H2 인메모리 DB 대신 실제 PostgreSQL을 사용하려면:

### 1. Docker Compose 실행
```bash
docker-compose up -d
```

### 2. application.yml 수정
```yaml
spring:
  profiles:
    active: postgres
```

또는 IntelliJ 실행 설정에서:
- `Run` → `Edit Configurations`
- `Active profiles`: `postgres` 입력

### 3. 데이터베이스 확인
```bash
docker exec -it nplusone-postgres psql -U nplusone -d nplusone

# psql 접속 후
\dt        # 테이블 목록
\d authors # authors 테이블 구조
```

## 🔧 트러블슈팅

### QueryDSL Q클래스를 찾을 수 없음
```
Cannot resolve symbol 'QAuthor'
```

**해결:**
1. Gradle 빌드 실행: `./gradlew build`
2. IntelliJ 캐시 무효화: `File` → `Invalidate Caches...` → `Invalidate and Restart`
3. Generated sources를 소스 폴더로 표시:
   - `build/generated/source/kapt/main` 우클릭
   - `Mark Directory as` → `Generated Sources Root`

### Gradle 동기화 실패
```
Could not resolve dependencies
```

**해결:**
1. 인터넷 연결 확인
2. Gradle 캐시 삭제:
   ```bash
   rm -rf ~/.gradle/caches
   ```
3. IntelliJ에서 다시 동기화: `File` → `Sync Project with Gradle Files`

### JDK 버전 에러
```
Unsupported class file major version
```

**해결:**
1. Java 17 설치 확인
2. IntelliJ 설정:
   - `File` → `Project Structure` → SDK를 Java 17로 변경
   - `Settings` → `Build Tools` → `Gradle` → Gradle JVM을 Java 17로 변경

## 📚 다음 단계

1. ✅ 테스트 실행으로 각 해결 방법 이해
2. ✅ 콘솔에서 실제 SQL 쿼리 확인
3. ✅ 성능 테스트로 각 방법 비교
4. 📝 [../report.md](../report.md)에 실험 결과 기록
5. 📝 [../troubleshooting.md](../troubleshooting.md)에 겪은 문제 기록

## 🎯 학습 포인트

각 테스트를 실행하면서 다음을 확인하세요:

1. **SQL 로그**: 실제로 몇 개의 쿼리가 실행되는지
2. **성능 차이**: 각 방법의 실행 시간 비교
3. **메모리 사용**: Fetch Join vs Batch Size
4. **코드 복잡도**: 각 방법의 구현 난이도

Happy Learning! 🚀
