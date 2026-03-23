# ApiDog 설정 가이드

이 문서는 [apidog-openapi.yaml](/Users/q/projects/playground/backend-lab/topics/online-qna-service/apidog-openapi.yaml)을 ApiDog에 import한 뒤, 로그인 응답의 `accessToken`을 환경변수에 저장하고 이후 인증 API에 재사용하는 방법을 정리합니다.

## 1. OpenAPI import

ApiDog에서 아래 파일을 import합니다.

- [apidog-openapi.yaml](/Users/q/projects/playground/backend-lab/topics/online-qna-service/apidog-openapi.yaml)

## 2. 환경변수 만들기

프로젝트 또는 환경 단위로 아래 변수를 만듭니다.

- `baseUrl`
- `bearerToken`
- `bearerTokenUser1`
- `bearerTokenUser2`
- `questionId`
- `answerId`

권장 초기값:

- `baseUrl = http://localhost:8080`
- 나머지는 비워둠

## 3. 공통 Authorization 설정

OpenAPI에는 `bearerAuth`만 정의되어 있으므로, ApiDog에서 실제 사용할 토큰 변수는 직접 연결해야 합니다.

권장 방식:

1. 프로젝트 또는 폴더 Authorization 타입을 `Bearer Token`으로 지정
2. 토큰 값에 `{{bearerToken}}` 입력

이렇게 해두면 현재 활성 토큰 변수만 바꿔도 인증이 필요한 API에 공통 적용할 수 있습니다.

## 4. 로그인 후처리 스크립트

## 방법 A. 단일 사용자 토큰 저장

로그인 API `POST /api/auth/login` 또는 `POST /api/auth/signup` 응답 후처리에 아래 스크립트를 넣습니다.

```javascript
const data = response.data;
api.environment.set("bearerToken", data.accessToken);
```

용도:

- 한 명만 테스트할 때 간단함

## 방법 B. user1 / user2 토큰 분리 저장

실습에서는 질문 작성자와 답변 작성자를 분리하는 경우가 많으므로 이 방식이 더 좋습니다.

### user1 로그인 API 후처리

```javascript
const data = response.data;
api.environment.set("bearerTokenUser1", data.accessToken);
api.environment.set("bearerToken", data.accessToken);
```

### user2 로그인 API 후처리

```javascript
const data = response.data;
api.environment.set("bearerTokenUser2", data.accessToken);
api.environment.set("bearerToken", data.accessToken);
```

의미:

- 각각 별도 사용자 토큰을 저장
- 동시에 현재 활성 토큰 `bearerToken`도 갱신

## 5. 질문 / 답변 ID 저장 후처리

실습 자동화를 위해 질문 작성, 답변 작성 후 ID도 저장해두는 것이 좋습니다.

### 질문 작성 API 후처리

대상:

- `POST /api/questions`

```javascript
const data = response.data;
api.environment.set("questionId", String(data.id));
```

### 답변 작성 API 후처리

대상:

- `POST /api/questions/{questionId}/answers`

```javascript
const data = response.data;
api.environment.set("answerId", String(data.id));
```

## 6. 토큰 전환용 헬퍼 요청

ApiDog에서 아래처럼 별도 요청 없이 환경변수만 바꾸는 간단한 스크립트 요청을 만들어두면 편합니다.

## user1 토큰 활성화

```javascript
api.environment.set("bearerToken", api.environment.get("bearerTokenUser1"));
```

## user2 토큰 활성화

```javascript
api.environment.set("bearerToken", api.environment.get("bearerTokenUser2"));
```

## 7. 추천 테스트 흐름

### 1. user1 회원가입 또는 로그인

- 후처리로 `bearerTokenUser1`, `bearerToken` 저장

### 2. user2 회원가입 또는 로그인

- 후처리로 `bearerTokenUser2` 저장

### 3. user1 토큰 활성화

```javascript
api.environment.set("bearerToken", api.environment.get("bearerTokenUser1"));
```

### 4. 질문 작성

- 후처리로 `questionId` 저장

### 5. 질문 목록 / 상세 조회

- Redis 캐시 확인

### 6. user2 토큰 활성화

```javascript
api.environment.set("bearerToken", api.environment.get("bearerTokenUser2"));
```

### 7. 답변 작성

- 후처리로 `answerId` 저장

### 8. user1 토큰 활성화

### 9. 답변 채택

### 10. 좋아요 / 신고 / 프로필 조회

## 8. 변수 사용 예시

질문 상세 조회:

- `GET {{baseUrl}}/api/questions/{{questionId}}`

답변 채택:

- `POST {{baseUrl}}/api/answers/{{answerId}}/accept`

body:

```json
{
  "questionId": {{questionId}}
}
```

질문 좋아요:

- `POST {{baseUrl}}/api/questions/{{questionId}}/vote`

```json
{
  "type": "LIKE"
}
```

## 9. 주의사항

- ApiDog 스크립트 객체 이름은 버전에 따라 약간 다를 수 있습니다.
- 만약 `api.environment.set(...)`가 동작하지 않으면, ApiDog UI의 내장 스크립트 예시 문법에 맞춰 같은 의미로 바꿔야 합니다.
- 핵심은 "로그인 응답의 `accessToken`을 환경변수에 저장"하는 것입니다.

## 10. 함께 보면 좋은 문서

- [practice-checklist.md](/Users/q/projects/playground/backend-lab/topics/online-qna-service/practice-checklist.md)
- [apidog-openapi.yaml](/Users/q/projects/playground/backend-lab/topics/online-qna-service/apidog-openapi.yaml)
