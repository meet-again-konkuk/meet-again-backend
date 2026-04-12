# Design: Email Value Object 도입

> 작성일: 2026-04-09
> 상태: Draft

## 1. 설계 개요

프로젝트 전반에서 `email: String`으로 사용되는 이메일 값을 `Email` Value Object로 원시값 포장하여, 타입 안전성을 확보하고 이메일 유효성 검증을 도메인 레이어에서 일관되게 수행한다.

## 2. 아키텍처

### 2.1 Email Value Object 배치 위치

`Email`은 `member` 도메인뿐 아니라 `matching`, `community`, `auth` 등 모든 도메인에서 사용되므로, **공통 도메인 패키지**(`common.domain`)에 배치한다.

```
domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/
  └── Email.kt
```

### 2.2 레이어별 변환 전략

```
┌─────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                        │
│                                                         │
│  Request DTO: email: String (Bean Validation 유지)      │
│    └── toCommand() 에서 Email(email)로 변환             │
│                                                         │
│  Controller: @AuthenticationPrincipal email: String      │
│    └── Email(email)로 변환 후 Service에 전달            │
│                                                         │
│  Response DTO: email: String (JSON 직렬화 유지)         │
│    └── Email.value로 언패킹                             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ domain/ma-domain-core                                   │
│                                                         │
│  Domain Model: email: Email (타입 안전)                 │
│  Port Interface: email: Email                           │
│  Application Service: email: Email                      │
│  Command: email: Email                                  │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ infrastructure                                          │
│                                                         │
│  JwtManager: email: String (JWT subject)                │
│    └── getEmailFromToken() → String 반환 유지           │
│    └── generateAccessToken(email: Email)                │
│         → email.value를 JWT subject로 저장              │
│                                                         │
│  Entity: email: String (DB 컬럼 매핑)                   │
│    └── toDomain()에서 Email(email)로 변환               │
│                                                         │
│  DAO: email: String (Exposed DSL 쿼리)                  │
│    └── Repository에서 email.value로 언패킹 후 전달      │
└─────────────────────────────────────────────────────────┘
```

### 2.3 경계별 변환 요약

| 경계 | 입력 | 출력 | 변환 위치 |
|------|------|------|-----------|
| HTTP Request → Controller | `String` | `Email` | Controller에서 `Email(email)` |
| JWT → Controller | `String` (principal) | `Email` | Controller에서 `Email(email)` |
| Controller → Service | `Email` | - | 이미 Email |
| Service → Port | `Email` | - | 이미 Email |
| Port → Repository(구현체) | `Email` | `String` | Repository에서 `email.value` |
| Repository → DAO | `String` | `String` | 그대로 전달 |
| DAO → Entity | `String` | `String` | Entity 생성 |
| Entity → Domain | `String` | `Email` | `toDomain()`에서 `Email(email)` |
| Domain → Response DTO | `Email` | `String` | Response에서 `email.value` |

## 3. 상세 설계

---

## 4. 구현 순서

의존성 순서를 고려하여 **도메인 → 인프라 → boot** 순으로 구현한다. 같은 레이어 내에서는 의존되는 쪽을 먼저 수정한다.

### Phase 1: Email Value Object 생성 + 테스트

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../common/domain/Email.kt` | 신규 | Email Value Object 클래스 |
| 2 | `domain/.../common/domain/EmailTest.kt` | 신규 | Email 유효성 검증 테스트 |

### Phase 2: Domain Model 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 3 | `domain/.../member/domain/Member.kt` | 수정 | `email: String` → `email: Email` |
| 4 | `domain/.../member/domain/NewMember.kt` | 수정 | `email: String` → `email: Email` |
| 5 | `domain/.../member/domain/Members.kt` | 수정 | email 파라미터/키 타입 변경 |
| 6 | `domain/.../member/domain/photo/MemberPhoto.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |
| 7 | `domain/.../member/domain/photo/MemberPhotos.kt` | 수정 | email 파라미터 타입 변경 |
| 8 | `domain/.../member/domain/photo/NewPhoto.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |
| 9 | `domain/.../member/domain/photo/MemberPhotoProcessor.kt` | 수정 | email 파라미터 타입 변경 |
| 10 | `domain/.../auth/domain/LoginInfo.kt` | 수정 | `email: String` → `email: Email` |
| 11 | `domain/.../auth/domain/RefreshToken.kt` | 수정 | `email: String` → `email: Email` |
| 12 | `domain/.../matching/domain/HasMatchingKey.kt` | 수정 | `targetEmail: String` → `targetEmail: Email` |
| 13 | `domain/.../matching/domain/MatchingResult.kt` | 수정 | registerEmail, targetEmail 타입 변경 |
| 14 | `domain/.../matching/domain/NewMatchingResult.kt` | 수정 | registerEmail, targetEmail 타입 변경 |
| 15 | `domain/.../matching/domain/Target.kt` | 수정 | `email: String` → `email: Email` |
| 16 | `domain/.../matching/domain/TargetInfo.kt` | 수정 | `registerEmail: String` → `registerEmail: Email` |
| 17 | `domain/.../matching/domain/NewTargetInfo.kt` | 수정 | `registerEmail: String` → `registerEmail: Email` |
| 18 | `domain/.../matching/domain/MatchingResults.kt` | 수정 | extractTargetEmails 반환 타입 변경 |
| 19 | `domain/.../community/domain/Comment.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 20 | `domain/.../community/domain/NewComment.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 21 | `domain/.../community/domain/Post.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 22 | `domain/.../community/domain/NewPost.kt` | 수정 | `authorEmail: String` → `authorEmail: Email` |
| 23 | `domain/.../community/domain/PostLike.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |
| 24 | `domain/.../community/domain/CommentLike.kt` | 수정 | `memberEmail: String` → `memberEmail: Email` |

### Phase 3: Domain Port 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 25 | `domain/.../member/domain/port/MemberQueryRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 26 | `domain/.../member/domain/port/MemberPhotoRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 27 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 28 | `domain/.../auth/domain/port/RefreshTokenRepository.kt` | 수정 | email 파라미터 타입 변경 |
| 29 | `domain/.../auth/domain/port/TokenManager.kt` | 수정 | email 파라미터/반환 타입 변경 |
| 30 | `domain/.../community/domain/port/PostLikeRepository.kt` | 수정 | memberEmail 파라미터 타입 변경 |
| 31 | `domain/.../community/domain/port/CommentLikeRepository.kt` | 수정 | memberEmail 파라미터 타입 변경 |

### Phase 4: Domain Command + Service + Component 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 32 | `domain/.../auth/application/command/SignUpCommand.kt` | 수정 | `email: String` → `email: Email` |
| 33 | `domain/.../auth/application/command/LoginCommand.kt` | 수정 | `email: String` → `email: Email` |
| 34 | `domain/.../auth/domain/SignUpValidator.kt` | 수정 | email 파라미터 타입 변경 |
| 35 | `domain/.../auth/domain/RefreshTokenGenerator.kt` | 수정 | email 파라미터 타입 변경 |
| 36 | `domain/.../auth/exception/PasswordMismatchException.kt` | 수정 | email 파라미터 타입 변경 |
| 37 | `domain/.../member/exception/DuplicateEmailException.kt` | 유지 | String 파라미터 유지 (로깅 목적) |

### Phase 5: Domain Application Service 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 38 | `domain/.../auth/application/LoginService.kt` | 수정 | 타입 전파에 따른 자동 변경 |
| 39 | `domain/.../auth/application/RefreshTokenService.kt` | 수정 | 타입 전파에 따른 자동 변경 |
| 40 | `domain/.../member/application/MemberPhotoService.kt` | 수정 | email 파라미터 타입 변경 |
| 41 | `domain/.../member/application/MemberQueryService.kt` | 수정 | email 파라미터 타입 변경 |
| 42 | `domain/.../matching/application/MatchingResultQueryService.kt` | 수정 | email 파라미터 타입 변경 |
| 43 | `domain/.../matching/application/MatchingResultCommandService.kt` | 수정 | email 파라미터 타입 변경 |
| 44 | `domain/.../community/application/CommentCommandService.kt` | 수정 | email 파라미터 타입 변경 |
| 45 | `domain/.../community/application/PostLikeService.kt` | 수정 | email 파라미터 타입 변경 |
| 46 | `domain/.../community/application/CommentLikeService.kt` | 수정 | email 파라미터 타입 변경 |

### Phase 6: Infrastructure Entity (toDomain 변환) 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 47 | `infra/.../matching/entity/MatchingResultEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 48 | `infra/.../auth/entity/RefreshTokenEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 49 | `infra/.../member/entity/MemberPhotoEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 50 | `infra/.../community/entity/PostEntity.kt` | 수정 | toDomain에서 Email 감싸기 |
| 51 | `infra/.../community/entity/CommentEntity.kt` | 수정 | toDomain에서 Email 감싸기 |

### Phase 7: Infrastructure DAO (.value 언패킹) 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 52 | `infra/.../matching/dao/MatchingResultCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 53 | `infra/.../member/dao/MemberCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 54 | `infra/.../member/dao/MemberPhotoCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 55 | `infra/.../auth/dao/RefreshTokenDao.kt` | 수정 | .value 언패킹 추가 |
| 56 | `infra/.../community/dao/PostCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 57 | `infra/.../community/dao/CommentCommandDao.kt` | 수정 | .value 언패킹 추가 |
| 58 | `infra/.../matching/dao/TargetInfoCommandDao.kt` | 수정 | .value 언패킹 추가 |

### Phase 8: Infrastructure Repository (.value 언패킹) 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 59 | `infra/.../matching/repository/MatchingResultCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 60 | `infra/.../member/repository/MemberQueryCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 61 | `infra/.../member/repository/MemberPhotoCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 62 | `infra/.../auth/repository/RefreshTokenCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 63 | `infra/.../community/repository/PostLikeCoreRepository.kt` | 수정 | .value 언패킹 추가 |
| 64 | `infra/.../community/repository/CommentLikeCoreRepository.kt` | 수정 | .value 언패킹 추가 |

### Phase 9: Infrastructure - JwtManager

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 65 | `infra/.../ma-jwt-core/.../JwtManager.kt` | 수정 | Email 파라미터/반환 타입 변경 |

### Phase 10: Boot - JwtAuthenticationFilter

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 66 | `boot/.../support/security/JwtAuthenticationFilter.kt` | 수정 | email.value로 principal 저장 |

### Phase 11: Boot - Controller 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 67 | `boot/.../matching/api/MatchingResultQueryApi.kt` | 수정 | Email(email) 변환 추가 |
| 68 | `boot/.../matching/api/MatchingResultCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 69 | `boot/.../member/api/MemberPhotoApi.kt` | 수정 | Email(email) 변환 추가 |
| 70 | `boot/.../matching/api/TargetInfoCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 71 | `boot/.../community/api/CommentCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 72 | `boot/.../community/api/PostCommandApi.kt` | 수정 | Email(email) 변환 추가 |
| 73 | `boot/.../community/api/PostLikeApi.kt` | 수정 | Email(email) 변환 추가 |
| 74 | `boot/.../community/api/CommentLikeApi.kt` | 수정 | Email(email) 변환 추가 |

### Phase 12: Boot - Request/Response DTO 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 75 | `boot/.../auth/api/request/SignUpRequest.kt` | 수정 | toCommand에서 Email 변환 + import alias |
| 76 | `boot/.../auth/api/request/LoginRequest.kt` | 수정 | toCommand에서 Email 변환 + import alias |
| 77 | `boot/.../member/api/request/DuplicatedEmailRequest.kt` | 수정 | toEmail() 변환 메서드 추가 |
| 78 | `boot/.../matching/api/request/NewTargetInfoRequest.kt` | 수정 | registerEmail 파라미터 타입 변경 |
| 79 | `boot/.../community/api/request/NewCommentRequest.kt` | 수정 | authorEmail 파라미터 타입 변경 |
| 80 | `boot/.../community/api/request/NewPostRequest.kt` | 수정 | authorEmail 파라미터 타입 변경 |
| 81 | `boot/.../auth/api/response/LoginResponse.kt` | 수정 | .value로 언패킹 |

### Phase 13: Test Fixture 변경

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 82 | `domain/.../member/fixture/NewMemberFixture.kt` | 수정 | Email(email) 변환 |
| 83 | `domain/.../matching/fixture/MemberFixture.kt` | 수정 | Email(email) 변환 |

### Phase 14: 기존 테스트 수정

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 84-97 | `boot/ma-boot-web/src/test/...` (약 14개 파일) | 수정 | 테스트 코드에서 email을 Email()로 감싸거나 .value 사용 |
| 98-107 | `infra/.../src/test/...` (약 10개 파일) | 수정 | DAO 테스트에서 email String 유지 확인 |

---

## 5. 고려사항

### 5.1 `jakarta.validation.constraints.Email`과 도메인 `Email` 이름 충돌

- Request DTO에서 `@Email` 어노테이션과 도메인 `Email` 클래스의 fully qualified name이 충돌
- **해결**: import alias 사용 — `import jakarta.validation.constraints.Email as EmailAnnotation`
- **대안**: 도메인 Email 클래스를 `EmailAddress`로 네이밍할 수도 있으나, 프로젝트 전반에서 `email`이라는 이름으로 사용되므로 `Email`이 더 자연스러움

### 5.2 Spring Security principal은 String 유지

- `@AuthenticationPrincipal email: String`은 Spring Security가 `SecurityContext`에 저장한 `principal`을 바인딩
- `JwtAuthenticationFilter`에서 `UsernamePasswordAuthenticationToken(email.value, ...)`로 String을 저장하므로 컨트롤러에서 String으로 받는 것이 안전
- 컨트롤러에서 `Email(email)`로 즉시 변환하여 서비스에 전달

### 5.3 `data class` 사용 이유

- `Email`을 `data class`로 선언하여 `equals()`, `hashCode()` 자동 생성
- `Map<Email, String>` (Members의 nicknameByEmail), `Set<Email>` (extractTargetEmails) 등에서 정상 동작 보장
- 기존 `FourDigit`도 `data class`로 선언되어 있어 패턴 일관성 유지

### 5.4 Email 정규식 엄격도

- 도메인 `Email`의 정규식은 기본적인 형식 검증만 수행 (@ 포함, 도메인 존재 여부)
- Bean Validation의 `@Email`이 이미 API 경계에서 더 상세한 검증을 수행
- 도메인 Email은 "프로그래밍 오류 방지" 수준의 가드 역할

### 5.5 성능 영향

- `Email` 객체 생성 시 정규식 검증이 추가되지만, 이미 Bean Validation에서 검증된 값이므로 실패 가능성 낮음
- `data class`이므로 객체 비교 시 `equals()`가 `value` 비교로 동작하여 기존 String 비교와 동일한 성능
- DAO 레벨에서 `.value`로 언패킹하므로 DB 쿼리에는 영향 없음

### 5.6 마이그레이션 안전성

- DB 스키마 변경 없음 (email 컬럼은 여전히 VARCHAR)
- DDL 변경 없음
- API 응답 형식 변경 없음 (Response DTO에서 `.value`로 String 반환)
- 기존 JWT 토큰 호환성 유지 (subject는 여전히 String)

### 5.7 TargetInfoEntity (batch에서 사용)

- TargetInfoEntity의 `toDomain()`에서도 `registerEmail`을 `Email`로 감싸야 함
- batch 모듈에서 TargetInfo를 사용하는 경우 해당 코드도 확인 필요
- 현재 코드에서는 batch Job에서 TargetInfo를 직접 생성하는 부분이 있으므로 추가 확인 필요

### 5.8 PasswordVerifier 확인 필요

- `PasswordVerifier.verify()` 메서드가 `Member`를 받으므로 `Member.email`이 `Email`로 변경되면 내부에서 예외 생성 시 `email.value` 전달 필요
- `PasswordMismatchException(email: Email)`로 변경하거나, `PasswordMismatchException(member.email.value)`로 호출

### 5.9 batch 모듈 영향 범위

- `boot/ma-boot-batch`에서 TargetInfo, MatchingResult 등을 사용하는 Job이 있을 수 있음
- Phase 구현 시 batch 모듈의 email 사용처도 반드시 확인 필요
