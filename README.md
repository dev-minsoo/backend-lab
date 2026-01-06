# 🔬 Backend Lab

> Kotlin과 Spring Boot로 백엔드 엔지니어링 문제를 체계적으로 학습하고 실험하는 저장소

## 🛠️ Tech Stack

- Kotlin 1.9.22
- Spring Boot 3.2.1
- Gradle (Kotlin DSL)
- JUnit 5, Kotest, MockK
- PostgreSQL, Redis, Kafka

## 📚 Topics

### JPA & Database
- 📝 [JPA N+1 문제](topics/jpa-n-plus-one) - Fetch Join, EntityGraph, Batch Size 비교
- 📝 영속성 컨텍스트 - 1차 캐시, 변경 감지, 플러시
- 📝 트랜잭션 격리 수준

### 동시성 제어
- 📝 Redis 분산 락 - Redisson vs Lettuce 비교
- 📝 Kafka Exactly-Once
- 📝 코루틴 동시성 제어

### 성능 최적화
- 📝 캐싱 전략
- 📝 배치 처리
- 📝 쿼리 최적화

## 📊 학습 현황

**전체 통계**
- 총 주제: 0개
- 완료: 0개 ✅
- 진행중: 0개 🚧
- 예정: 0개 📝

**최근 업데이트**
- 2026-01-03: 프로젝트 초기 설정 완료

## 🎯 학습 목표

- [ ] JPA 최적화 마스터
- [ ] 동시성 제어 완벽 이해
- [ ] 성능 튜닝 실전 경험
- [ ] 10개 주제 완성

## 💡 주요 인사이트

새로운 주제를 학습하고 여기에 인사이트를 추가하세요!

## 🔗 Links

- [프로젝트 구조 가이드](PROJECT_STRUCTURE.md)
- [템플릿 디렉토리](templates/)

## 📝 사용 방법

### 새로운 주제 시작하기
```bash
# 주제 디렉토리 생성
mkdir -p topics/{주제명}/implementation

# 템플릿 복사
cp templates/topic-readme-template.md topics/{주제명}/README.md
cp templates/report-template.md topics/{주제명}/report.md
cp templates/troubleshooting-template.md topics/{주제명}/troubleshooting.md
```

### 테스트 실행
```bash
cd topics/{topic-name}/implementation
./gradlew test
```

### Docker 환경 시작
```bash
cd topics/{topic-name}/implementation
docker-compose up -d
```

## 📜 License

MIT License - 학습 목적으로 자유롭게 사용 가능합니다.
