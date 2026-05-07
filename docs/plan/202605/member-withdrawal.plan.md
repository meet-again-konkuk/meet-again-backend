# Plan: 회원 탈퇴 (Account Deletion) — 7일 유예 + Batch Step 모델

> 작성일: 2026-05-03
> 상태: Draft
> 변경 이력:
> - 2026-05-03 즉시 익명화 → 7일 유예 + 배치 익명화 모델
> - 2026-05-06 도메인 이벤트 패턴 도입 (양방향 의존 회피)
> - 2026-05-07 도메인 이벤트 패턴 폐기 → **RPW + 도메인별 Step** 모델로 변경. 즉시 부수 효과 제거 (Member 상태 변경만)

## 1. 개요

앱스토어/구글플레이 심사를 위한 in-app account deletion API를 구현한다 (Apple Guideline 5.1.1(v), Google Play 데이터 안전 정책).

- **모델**: **7일 유예 기간 + 배치 Step 익명화**
  - 사용자가 탈퇴 신청 시 `Member.withdrawalRequestedAt`만 set
  - 7일 동안 다른 사용자에게 안 보이고, 본인 액션도 차단됨 (조회 필터 + 인터셉터)
  - 7일 동안 명시적 복구(취소) 가능
  - 7일 경과 시 배치가 도메인별 Step으로 cleanup + 익명화
- **즉시 부수 효과 없음**: 탈퇴 신청 시 RefreshToken 삭제 등 외부 도메인 작업을 하지 않음. 인터셉터/쿼리 필터로 충분히 차단됨. RefreshToken은 7일 후 배치에서 함께 정리
- **도메인 협력**: 도메인 모듈끼리 추가 의존 없음. cleanup 작업은 모두 `boot/ma-boot-batch` 안의 Step이 도메인별 cleanup port를 호출하는 방식
- **유예 기간 정책 (확정)**:
  - 유예 중 로그인 시도 → **명시적 복구** (특수 응답 → 클라이언트 복구 화면 → 복구 API 호출)
  - 유예 중 같은 이메일/전화번호로 재가입 → **차단**
  - 유예 중 다른 사용자에게 노출 → **즉시 숨김** (조회 쿼리 필터 + 응답 변환)
  - 유예 중 본인 액션 (충전/매칭/글쓰기 등) → **모두 차단** (인터셉터)
- **응답**: `204 No Content` (탈퇴 신청), `200` (복구)
- **상수**: `WithdrawalPolicy.GRACE_PERIOD_DAYS = 7`

### 1.1 도메인 분리

새 도메인을 만들지 않고 기존 `member` 도메인 안에 application 서비스를 추가:

- `MemberWithdrawalService` — 탈퇴 신청 (`Member.withdrawalRequestedAt = now`만)
- `MemberWithdrawalCancelService` — 복구

**도메인 간 협력은 도메인 모듈에서 일으키지 않는다.** cleanup은 모두 `boot/ma-boot-batch` 모듈에서 처리:
- batch 모듈은 원래 모든 도메인 의존하는 게 자연스러움
- 도메인 모듈끼리는 `auth → member` 단방향만 유지

---

## 2. API 설계

| Method | Endpoint | 용도 | Content-Type | 인증 |
|--------|----------|------|--------------|------|
| DELETE | `/api/members/me` | 회원 탈퇴 신청 | application/json | 필요 |
| POST | `/api/members/me/withdrawal/cancel` | 탈퇴 신청 복구 | - | 필요 (특수) |

### 2.1 DELETE `/api/members/me` — 탈퇴 신청

#### Request Body
```json
{ "password": "P@ssword123" }
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `password` | String | Y (자체 회원) | 보안상 비밀번호 재확인 |

> **소셜 로그인 도입 시점에 분기 필요** — 추후 `Member.loginType` 필드 도입 시 `MemberWithdrawalValidator`에 한 줄 분기 추가.

#### Response
- **성공**: `204 No Content`
- **실패**:
  - 400: 비밀번호 누락
  - 401: 비밀번호 불일치
  - 409: 이미 탈퇴 신청 중 (`AlreadyWithdrawalRequestedException`)

#### 동작

1. 비밀번호 검증 (`MemberWithdrawalValidator`)
2. `Member.requestWithdrawal(now)` → save
3. **그게 끝** — 즉시 부수 효과 없음. RefreshToken 등은 7일 후 배치가 정리

> **노출 차단**: `Member.withdrawalRequestedAt`이 set되는 즉시:
> - `WithdrawalGuardInterceptor`가 본인 모든 액션 차단
> - 매칭/Claim/TargetInfo 쿼리 필터가 결과에서 제외
> - 게시글/댓글 작성자 닉네임 응답 변환
>
> RefreshToken이 살아있어도 인터셉터로 무력화 (갱신해서 새 access token 받아도 차단됨).

### 2.2 POST `/api/members/me/withdrawal/cancel` — 복구

#### Response
- **성공**: `200 OK`
  ```json
  {
    "email": "user@konkuk.ac.kr",
    "nickname": "닉네임",
    "cancelledAt": "2026-05-04T10:30:00",
    "accessToken": "...",
    "refreshToken": "..."
  }
  ```
- **실패**: 400 (탈퇴 신청 중 아님), 410 (유예 만료, 이미 익명화됨)

#### 동작
1. `Member.cancelWithdrawal()` → save (`withdrawalRequestedAt = null`)
2. 새 access/refresh token 발급

> **로그인 흐름**: `LoginService`가 `member.isWithdrawalPending(now)` 체크 → `LoginInfo.WithdrawalPending` 응답. 클라이언트는 응답 받아 복구 화면 → 사용자가 복구 버튼 누르면 cancel API 호출.

---

## 3. 처리 전략 — 시점별

### 3.1 즉시 처리 (탈퇴 신청 시점)

`MemberWithdrawalService.requestWithdrawal`이 단일 트랜잭션에서:
1. 비밀번호 검증
2. `Member.requestWithdrawal(now)` → save

**그 외 부수 효과 없음**. 노출 차단은 다음으로 자동 작동:
- 인터셉터(`WithdrawalGuardInterceptor`)가 본인 액션 차단
- 조회 쿼리 필터 + 응답 변환이 다른 사용자 노출 차단

### 3.2 유예 후 처리 (배치, RPW + 도메인별 Step)

```
MemberWithdrawalCompleteJob (cron 0 0 3 * * *)
  ├ Step 1: AuthCleanupStep            → AuthCleanupItemWriter
  ├ Step 2: MatchingCleanupStep        → MatchingCleanupItemWriter
  ├ Step 3: PointCleanupStep           → PointCleanupItemWriter
  ├ Step 4: CommunityCleanupStep       → CommunityCleanupItemWriter
  ├ Step 5: SupportCleanupStep         → SupportCleanupItemWriter
  ├ Step 6: XroomCleanupStep           → XroomCleanupItemWriter
  ├ Step 7: MemberPhotoCleanupStep     → MemberPhotoCleanupItemWriter
  └ Step 8: MemberAnonymizeStep        → MemberAnonymizeItemWriter (마지막)
```

각 Step:
- **Reader**: `ExpiredWithdrawalMemberItemReader` (페이지 100, `findExpiredWithdrawalRequests(now, pageable)`)
  - 모든 Step이 같은 클래스 사용 (Spring Batch가 step별로 재인스턴스화)
- **Writer**: 도메인별 신규 클래스. chunk(`List<Member>`)를 받아 자기 도메인 cleanup port 호출
- **Processor**: 사용 안 함 (변환 작업 없음, PassThrough도 의미 없음)
- **Chunk size**: 100 (회원 100명을 한 트랜잭션에서 처리)

> **회원 단위 atomic이 아닌 chunk 단위 atomic**:
> - 한 chunk 안에서 일부 회원 실패 시 chunk 전체 롤백
> - 다음 batch 실행 시 같은 회원이 다시 조회되어 재처리 (멱등)
> - cleanup 작업은 모두 멱등이라 일관성 회복 가능
> - **노출 측면**: `Member.withdrawalRequestedAt`이 set된 채 보존되므로 모든 노출 필터/인터셉터가 계속 작동 → 부분 처리 상태에서도 사용자 노출은 안전

> **Step 순서**: `MemberAnonymizeStep`은 마지막. Member가 익명화된 후에는 다른 도메인 cleanup이 회원 조회를 못 하므로 Member는 가장 마지막에 처리.

### 3.3 도메인별 처리 매트릭스

| 도메인 | 테이블 | 식별 컬럼 | 처리 | 처리 위치 |
|--------|--------|-----------|------|----------|
| **member** | `MEMBERS` | `EMAIL` | 익명화 + soft delete | `MemberAnonymizeItemWriter` |
| **member** | `MEMBER_PHOTOS` | `MEMBER_EMAIL` | soft delete + 파일 삭제 | `MemberPhotoCleanupItemWriter` |
| **auth** | `REFRESH_TOKENS` | `EMAIL` | hard delete | `AuthCleanupItemWriter` |
| **matching** | `TARGET_INFOS` | `REGISTER_EMAIL` | soft delete | `MatchingCleanupItemWriter` |
| **matching** | `MATCHING_RESULTS` | `REGISTER_EMAIL`/`TARGET_EMAIL` | soft delete (양방향) | 동일 |
| **point** | `MEMBER_POINTS` | `OWNER_EMAIL` (unique) | 잔액 0 + 익명화 + soft delete | `PointCleanupItemWriter` |
| **point** | `POINT_HISTORIES` | `OWNER_EMAIL` | 익명화 (이메일만) | 동일 |
| **community** | `COMMUNITY_POSTS` | `AUTHOR_EMAIL` | 익명화 (작성자만) | `CommunityCleanupItemWriter` |
| **community** | `COMMUNITY_COMMENTS` | `AUTHOR_EMAIL` | 익명화 (작성자만) | 동일 |
| **community** | `COMMUNITY_POST_LIKES` | `MEMBER_EMAIL` | hard delete | 동일 |
| **community** | `COMMUNITY_COMMENT_LIKES` | `MEMBER_EMAIL` | hard delete | 동일 |
| **support** | `INQUIRIES` | `AUTHOR_EMAIL` | 익명화 | `SupportCleanupItemWriter` |
| **xroom** | `XROOMS` | `OWNER_EMAIL` | soft delete | `XroomCleanupItemWriter` |

### 3.4 즉시 숨김 처리 (유예 기간 동안)

| 데이터 종류 | 처리 방식 | 위치 |
|-------------|----------|------|
| **매칭 결과** | DB 쿼리 필터 — 상대방 Member의 `withdrawalRequestedAt IS NULL` 조건 추가 | `MatchingResultQueryDao` |
| **TargetInfo 후보** | DB 쿼리 필터 — register Member 동일 | `TargetInfoQueryDao` |
| **Claimer 목록** | DB 쿼리 필터 — claimer Member 동일 | `ClaimerQueryDao` |
| **게시글/댓글 작성자** | 응답 변환 — `WithdrawnNicknameMasker.mask(member)` | `Post`/`Comment` query 매퍼 |

### 3.5 익명화 규칙 (배치 시점)

| 컬럼 | 익명화 후 값 |
|------|--------------|
| `MEMBERS.email` | `withdrawn_{memberId}@deleted.local` |
| `MEMBERS.nickname` | `탈퇴한회원_{memberId}` |
| `MEMBERS.password` | 빈 문자열 또는 무효 해시 |
| `MEMBERS.phoneNumber` | `00000000000` |
| `MEMBERS.name` | `탈퇴한회원` |
| `MEMBERS.birthDate` | `1900-01-01` |
| `MEMBERS.region` | sentinel |
| `MEMBERS.highSchool` / `university` / `profileImageUrl` | `null` |
| `MEMBERS.withdrawalRequestedAt` | 보존 (감사용) |
| `COMMUNITY_POSTS.authorEmail` 등 외부 작성자 컬럼 | `withdrawn_{memberId}@deleted.local` |

> **하드코딩 회피**: sentinel 값은 `WithdrawnSentinel` object 상수.

---

## 4. 변경 전략

### 4.1 도메인 모델 / 정책

| 클래스 | 변경 | 책임 |
|--------|------|------|
| `Member` | 수정 | `withdrawalRequestedAt: LocalDateTime?` 필드 추가. `requestWithdrawal(now)`, `cancelWithdrawal()`, `isWithdrawalPending(now)`, `isWithdrawalExpired(now)`, `isActive()`, `anonymize(memberId)` 메서드 |
| `WithdrawalPolicy` | 신규 | object. `GRACE_PERIOD_DAYS = 7L`, `expiresAt(requestedAt)` |
| `WithdrawnSentinel` | 신규 | 익명화 sentinel 상수 |
| `WithdrawnNicknameMasker` | 신규 | `mask(member: Member): String` |
| `Email.withdrawn(memberId)` | 수정 | 팩토리 |
| `AlreadyWithdrawalRequestedException` | 신규 | 도메인 예외 |
| `NotWithdrawalRequestedException` | 신규 | 도메인 예외 |
| `WithdrawalPendingMemberException` | 신규 | 재가입 차단 |

### 4.2 application — 신청 / 복구

| 클래스 | 위치 | 책임 |
|--------|------|------|
| `WithdrawalRequestCommand` | `member.application.command` | `data class(email, password)` |
| `MemberWithdrawalValidator` | `member.domain` | `validate(member, password)` — 비번 + 이미 pending 차단 |
| `MemberWithdrawalCancelValidator` | `member.domain` | `validate(member)` — pending 상태 검증 |
| `MemberWithdrawalService` | `member.application` | `@Service @Transactional`. `requestWithdrawal(command)` — 검증 → `Member.requestWithdrawal` → save (그게 전부) |
| `MemberWithdrawalCancelService` | `member.application` | `@Service @Transactional`. `cancel(email): LoginInfo` — Member 업데이트 → 새 토큰 발급 |

> **도메인 간 협력 없음** — Service는 자기 도메인 내부 로직만 수행. cleanup은 batch가 담당.

### 4.3 로그인 흐름 변경

| 클래스 | 변경 | 내용 |
|--------|------|------|
| `LoginInfo` | 수정 (sealed) | `LoginInfo.Active(...)`, `LoginInfo.WithdrawalPending(accessToken, refreshToken, withdrawalRequestedAt, withdrawalExpiresAt)` |
| `LoginService` | 수정 | 비번 검증 후 `member.isWithdrawalPending(now)` 분기 |
| `LoginApi` | 수정 | sealed 분기에 따라 응답 매핑. `status: "ACTIVE" \| "WITHDRAWAL_PENDING"` 필드 |

### 4.4 액션 차단 (유예 중)

| 클래스 | 변경 | 내용 |
|--------|------|------|
| `WithdrawalGuardInterceptor` | 신규 | `HandlerInterceptor`. 인증된 사용자의 `Member.withdrawalRequestedAt` 체크 → not null이면 403. `@SkipWithdrawalGuard`는 통과 |
| `@SkipWithdrawalGuard` | 신규 | annotation. cancel 엔드포인트 부착 |
| `WebMvcConfig` | 수정 | 인터셉터 등록 |

### 4.5 재가입 차단

| 클래스 | 변경 | 내용 |
|--------|------|------|
| `SignUpValidator` | 수정 | 이메일/전화번호 unique 검증 시 pending 회원 포함. 발견 시 `WithdrawalPendingMemberException` |
| `MemberQueryRepository.existsActiveOrPendingByEmail` | 신규 메서드 | pending 포함 |
| `MemberQueryRepository.existsActiveOrPendingByPhoneNumber` | 신규 메서드 | 동일 |
| 이메일/닉네임 중복 확인 API | 수정 | 동일 정책 |

### 4.6 즉시 숨김 — 조회 필터/응답 변환

| 위치 | 변경 |
|------|------|
| `MatchingResultQueryDao.findResults` | 상대방 Member `withdrawalRequestedAt IS NULL` 조건 |
| `TargetInfoQueryDao.findCandidates` | register Member 동일 |
| `ClaimerQueryDao.findMyClaimers` | claimer Member 동일 |
| `Post`/`Comment` query 응답 매퍼 | `WithdrawnNicknameMasker.mask(member)` 호출 |

### 4.7 배치 — RPW + 도메인별 Step

| 클래스 | 위치 | 책임 |
|--------|------|------|
| `MemberWithdrawalCompleteJobConfig` | `boot/ma-boot-batch/.../job/domain/member` | Job + 8 Step bean 정의. cron `0 0 3 * * *`. Step 순서 보장 |
| `ExpiredWithdrawalMemberItemReader` | 동일 | `ItemReader<Member>`. 페이지 100. `findExpiredWithdrawalRequests(now, pageable)`. 모든 Step에서 재사용 |
| `AuthCleanupItemWriter` | 동일 | chunk 받아 회원별 RefreshToken hard delete |
| `MatchingCleanupItemWriter` | 동일 | TargetInfo `deleteByRegisterEmail` + MatchingResult `deleteByMember` |
| `PointCleanupItemWriter` | 동일 | MemberPoint `deleteByOwner` + PointHistory `anonymizeOwner` |
| `CommunityCleanupItemWriter` | 동일 | Post/Comment `anonymizeAuthor` + PostLike/CommentLike `deleteByMember` |
| `SupportCleanupItemWriter` | 동일 | Inquiry `anonymizeAuthor` |
| `XroomCleanupItemWriter` | 동일 | Xroom `deleteByOwner` |
| `MemberPhotoCleanupItemWriter` | 동일 | MemberPhoto 파일 + DB 삭제 (`MemberPhotoCleaner` 위임) |
| `MemberAnonymizeItemWriter` | 동일 | Member `anonymizeAndSoftDelete` (마지막 Step) |
| `MemberQueryRepository.findExpiredWithdrawalRequests(now, pageable)` | port | 신규 메서드 |

> **chunk size = 100**: 트랜잭션 단위. 100명을 한 번에 처리하다가 한 명 실패 시 chunk 전체 롤백 → 다음 배치에서 재처리. 멱등이라 일관성 회복.
> **Step 간 데이터 공유 안 함**: 각 Step의 Reader가 동일한 `findExpiredWithdrawalRequests`를 다시 호출. 같은 SQL이 8번 실행되지만 인덱스 있어 부담 적음. 단순함을 우선.
> **Processor 사용 안 함**: 변환 로직 없음.
> **SkipPolicy 적용**: 회원 1명 실패 시 전체 chunk 롤백되지만, 같은 회원이 반복 실패하면 skip 처리 후 운영자 알림.

### 4.8 트랜잭션 / 동시성

- **탈퇴 신청** (`MemberWithdrawalService.requestWithdrawal`): 단일 `@Transactional`. Member update만. 외부 도메인 작업 없음
- **복구** (`MemberWithdrawalCancelService.cancel`): 단일 `@Transactional`. Member update + 새 토큰 발급
- **배치**: chunk 단위 트랜잭션 (Spring Batch 기본). 회원 단위 atomic은 깨지지만 멱등성 회복
- **노출 안전**: 부분 처리 상태에서도 `Member.withdrawalRequestedAt`이 보존되어 모든 즉시 숨김 필터/인터셉터가 작동
- **Step 순서 보장**: Spring Batch Job이 step 순차 실행 (`.next()`) — `MemberAnonymizeStep`이 마지막에 실행되어 다른 도메인 cleanup이 Member 조회 가능

### 4.9 토큰 무효화

- **RefreshToken**: 7일 후 `AuthCleanupStep`이 hard delete. 신청 즉시 삭제 안 함
- **AccessToken**: stateless라 만료까지 유효하지만 `WithdrawalGuardInterceptor`가 모든 액션 차단
- **복구 시**: 새 access + refresh token 발급. 기존 refresh token은 그대로 (DB에 남아있음). 추후 갱신 시도 시 새로 받은 토큰으로 갱신

---

## 5. 변경 파일 목록

### Phase 1: 도메인 모델 / 정책

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 1 | `domain/.../member/domain/Member.kt` | 수정 | `withdrawalRequestedAt: LocalDateTime?` + 메서드 추가 |
| 2 | `domain/.../member/domain/policy/WithdrawalPolicy.kt` | 신규 | object. `GRACE_PERIOD_DAYS = 7L`, `expiresAt(requestedAt)` |
| 3 | `domain/.../member/domain/policy/WithdrawnSentinel.kt` | 신규 | sentinel 상수 |
| 4 | `domain/.../member/domain/WithdrawnNicknameMasker.kt` | 신규 | `mask(member: Member): String` |
| 5 | `domain/.../common/domain/Email.kt` | 수정 | `companion`에 `withdrawn(memberId): Email` |
| 6 | `domain/.../member/domain/exception/AlreadyWithdrawalRequestedException.kt` | 신규 | 예외 |
| 7 | `domain/.../member/domain/exception/NotWithdrawalRequestedException.kt` | 신규 | 예외 |
| 8 | `domain/.../member/domain/exception/WithdrawalPendingMemberException.kt` | 신규 | 예외 |

### Phase 2: 도메인 서비스 — 신청 / 복구

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 9 | `domain/.../member/domain/MemberWithdrawalValidator.kt` | 신규 | `validate(member, password)` |
| 10 | `domain/.../member/domain/MemberWithdrawalCancelValidator.kt` | 신규 | `validate(member)` |
| 11 | `domain/.../member/application/command/WithdrawalRequestCommand.kt` | 신규 | `data class(email, password)` |
| 12 | `domain/.../member/application/MemberWithdrawalService.kt` | 신규 | 검증 → Member update만 |
| 13 | `domain/.../member/application/MemberWithdrawalCancelService.kt` | 신규 | 검증 → Member update → 새 토큰 |
| 14 | `domain/.../member/domain/photo/MemberPhotoCleaner.kt` | 신규 | 사진 파일+DB 삭제 로직 추출 (web 즉시 삭제용 + batch 익명화용 공유) |
| 15 | `domain/.../member/application/MemberPhotoService.kt` | 수정 | 내부 로직을 `MemberPhotoCleaner` 위임 |

### Phase 3: 로그인 / 인증 흐름

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 16 | `domain/.../auth/domain/LoginInfo.kt` | 수정 | sealed로. `Active`, `WithdrawalPending` |
| 17 | `domain/.../auth/application/LoginService.kt` | 수정 | pending 분기 |

### Phase 4: 도메인 포트 (cleanup / 조회용)

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 18 | `domain/.../member/domain/port/MemberCommandRepository.kt` | 수정 | `requestWithdrawal(member, requestedAt)` / `cancelWithdrawal(member)` / `anonymizeAndSoftDelete(member)` |
| 19 | `domain/.../member/domain/port/MemberQueryRepository.kt` | 수정 | `findExpiredWithdrawalRequests(now, pageable)`, `existsActiveOrPendingByEmail`, `existsActiveOrPendingByPhoneNumber` |
| 20 | `domain/.../matching/domain/port/TargetInfoCommandRepository.kt` | 수정 | `deleteByRegisterEmail(email)` |
| 21 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `deleteByMember(email)` 양방향 |
| 22 | `domain/.../point/domain/port/MemberPointRepository.kt` | 수정 | `deleteByOwner(email)` |
| 23 | `domain/.../point/domain/port/PointHistoryRepository.kt` | 수정 | `anonymizeOwner(oldEmail, newEmail)` |
| 24 | `domain/.../community/domain/port/PostCommandRepository.kt` | 수정 | `anonymizeAuthor(oldEmail, newEmail)` |
| 25 | `domain/.../community/domain/port/CommentCommandRepository.kt` | 수정 | `anonymizeAuthor(oldEmail, newEmail)` |
| 26 | `domain/.../community/domain/port/PostLikeRepository.kt` | 수정 | `deleteByMember(email)` (hard) |
| 27 | `domain/.../community/domain/port/CommentLikeRepository.kt` | 수정 | `deleteByMember(email)` (hard) |
| 28 | `domain/.../support/domain/port/InquiryCommandRepository.kt` | 수정 | `anonymizeAuthor(oldEmail, newEmail)` |
| 29 | `domain/.../xroom/domain/port/XroomCommandRepository.kt` | 수정 | `deleteByOwner(email)` |
| 30 | `domain/.../auth/domain/port/RefreshTokenRepository.kt` | 확인/수정 | `deleteByEmail(email)` 보유 여부 확인. 없으면 추가 |

### Phase 5: 즉시 숨김 — 쿼리/응답

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 31 | `infra/.../matching/dao/MatchingResultQueryDao.kt` | 수정 | 상대방 Member `withdrawalRequestedAt IS NULL` 조건 |
| 32 | `infra/.../matching/dao/TargetInfoQueryDao.kt` | 수정 | register Member 동일 |
| 33 | `infra/.../matching/dao/ClaimerQueryDao.kt` | 수정 | claimer Member 동일 |
| 34 | `boot/.../community/api/(Post\|Comment) 응답 매퍼` | 수정 | `WithdrawnNicknameMasker.mask` 호출 |

### Phase 6: 액션 차단 / 재가입 차단

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 35 | `boot/.../config/security/WithdrawalGuardInterceptor.kt` | 신규 | `HandlerInterceptor` |
| 36 | `boot/.../config/security/SkipWithdrawalGuard.kt` | 신규 | annotation |
| 37 | `boot/.../config/WebMvcConfig.kt` | 수정 | 인터셉터 등록 |
| 38 | `domain/.../auth/domain/SignUpValidator.kt` | 수정 | pending 포함 unique 검증 |
| 39 | `domain/.../member/application/MemberQueryService.kt` | 수정 | 이메일/닉네임/전화 중복 확인 동일 정책 |

### Phase 7: 인프라 — Table / DAO

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 40 | `infra/.../member/entity/table/MemberTable.kt` | 수정 | `WITHDRAWAL_REQUESTED_AT` datetime nullable + 인덱스 |
| 41 | `infra/.../member/entity/MemberEntity.kt` | 수정 | 매핑 |
| 42 | `infra/.../member/dao/MemberCommandDao.kt` | 수정 | `requestWithdrawal`, `cancelWithdrawal`, `anonymizeAndSoftDelete` |
| 43 | `infra/.../member/dao/MemberQueryDao.kt` | 수정 | `findExpiredWithdrawalRequests`, `existsActiveOrPendingByEmail`, `existsActiveOrPendingByPhoneNumber` |
| 44 | `infra/.../matching/dao/TargetInfoCommandDao.kt` | 수정 | `deleteByRegisterEmail` |
| 45 | `infra/.../matching/dao/MatchingResultCommandDao.kt` | 수정 | `deleteByMember` 양방향 |
| 46 | `infra/.../point/dao/MemberPointCommandDao.kt` | 수정/신규 | `deleteByOwner` |
| 47 | `infra/.../point/dao/PointHistoryDao.kt` | 수정 | `anonymizeOwner` |
| 48 | `infra/.../community/dao/PostCommandDao.kt` | 수정 | `anonymizeAuthor` |
| 49 | `infra/.../community/dao/CommentCommandDao.kt` | 수정 | `anonymizeAuthor` |
| 50 | `infra/.../community/dao/PostLikeDao.kt` | 수정 | `deleteByMember` (hard) |
| 51 | `infra/.../community/dao/CommentLikeDao.kt` | 수정 | `deleteByMember` (hard) |
| 52 | `infra/.../support/dao/InquiryCommandDao.kt` | 수정 | `anonymizeAuthor` |
| 53 | `infra/.../xroom/dao/XroomCommandDao.kt` | 수정 | `deleteByOwner` |
| 54 | `infra/.../auth/dao/RefreshTokenDao.kt` | 확인/수정 | `deleteByEmail(email)` |

### Phase 8: 인프라 — Repository (포트 구현)

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 55 | `infra/.../member/repository/MemberCommandCoreRepository.kt` | 수정 | 신규 메서드 구현 |
| 56 | `infra/.../member/repository/MemberQueryCoreRepository.kt` | 수정 | 신규 메서드 구현 |
| 57~65 | 각 도메인 `*CoreRepository.kt` | 수정 | 신규 포트 메서드 DAO 위임 (9개) |

### Phase 9: Boot — Web API

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 66 | `boot/.../member/api/request/WithdrawalRequest.kt` | 신규 | `data class(password: String)` + `@field:NotBlank` |
| 67 | `boot/.../member/api/response/WithdrawalCancelResponse.kt` | 신규 | email, nickname, cancelledAt, accessToken, refreshToken |
| 68 | `boot/.../member/api/MemberWithdrawalApi.kt` | 신규 | DELETE/cancel. cancel은 `@SkipWithdrawalGuard` |
| 69 | `boot/.../auth/api/response/LoginResponse.kt` | 수정 | `status` 필드, `withdrawalRequestedAt`, `withdrawalExpiresAt` nullable |
| 70 | `boot/.../auth/api/LoginApi.kt` | 수정 | sealed 분기 매핑 |

### Phase 10: Boot — Batch (RPW + 도메인별 Step)

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 71 | `boot/ma-boot-batch/.../job/domain/member/MemberWithdrawalCompleteJobConfig.kt` | 신규 | Job + 8 Step bean 정의. chunk size = 100. SkipPolicy. cron `0 0 3 * * *` |
| 72 | `boot/ma-boot-batch/.../job/domain/member/ExpiredWithdrawalMemberItemReader.kt` | 신규 | `ItemReader<Member>` 페이징 |
| 73 | `boot/ma-boot-batch/.../job/domain/member/AuthCleanupItemWriter.kt` | 신규 | RefreshToken hard delete |
| 74 | `boot/ma-boot-batch/.../job/domain/member/MatchingCleanupItemWriter.kt` | 신규 | TargetInfo + MatchingResult cleanup |
| 75 | `boot/ma-boot-batch/.../job/domain/member/PointCleanupItemWriter.kt` | 신규 | MemberPoint + PointHistory |
| 76 | `boot/ma-boot-batch/.../job/domain/member/CommunityCleanupItemWriter.kt` | 신규 | Post + Comment + PostLike + CommentLike |
| 77 | `boot/ma-boot-batch/.../job/domain/member/SupportCleanupItemWriter.kt` | 신규 | Inquiry |
| 78 | `boot/ma-boot-batch/.../job/domain/member/XroomCleanupItemWriter.kt` | 신규 | Xroom |
| 79 | `boot/ma-boot-batch/.../job/domain/member/MemberPhotoCleanupItemWriter.kt` | 신규 | MemberPhoto 파일+DB |
| 80 | `boot/ma-boot-batch/.../job/domain/member/MemberAnonymizeItemWriter.kt` | 신규 | Member 익명화 + soft delete (마지막 Step) |
| 81 | `boot/ma-boot-batch/.../job/domain/member/MemberWithdrawalCompleteJobScheduler.kt` (선택) | 신규 | 스케줄러 트리거 |

### Phase 11: 테스트

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 82 | `domain/.../member/domain/MemberWithdrawalTest.kt` | 신규 | requestWithdrawal/cancelWithdrawal/isWithdrawalPending/isWithdrawalExpired/anonymize |
| 83 | `domain/.../member/domain/policy/WithdrawnSentinelTest.kt` | 신규 | sentinel 형식 |
| 84 | `domain/.../common/domain/EmailWithdrawnTest.kt` | 신규 | 형식/정규식 통과 |
| 85 | `domain/.../member/domain/MemberWithdrawalValidatorTest.kt` | 신규 | 비번 일치/불일치/이미 pending |
| 86 | `domain/.../member/domain/MemberWithdrawalCancelValidatorTest.kt` | 신규 | active/pending/expired |
| 87 | `domain/.../member/application/MemberWithdrawalServiceTest.kt` | 신규 | Member update verify (외부 부수 효과 없음 검증) |
| 88 | `domain/.../member/application/MemberWithdrawalCancelServiceTest.kt` | 신규 | Member 복구 + 새 토큰 발급 |
| 89 | `domain/.../auth/application/LoginServiceTest.kt` | 수정 | active/withdrawal_pending 분기 |
| 90 | `domain/.../auth/domain/SignUpValidatorTest.kt` | 수정 | pending 회원 재가입 차단 |
| 91 | `boot/.../member/api/MemberWithdrawalApiTest.kt` | 신규 | REST Docs (DELETE 204, POST cancel 200, 401, 409, 400) |
| 92 | `boot/.../auth/api/LoginApiTest.kt` | 수정 | withdrawal_pending 응답 REST Docs |
| 93 | `boot/.../config/security/WithdrawalGuardInterceptorTest.kt` | 신규 | 차단/통과 |
| 94 | `infra/.../member/dao/MemberCommandDaoWithdrawalTest.kt` | 신규 | DDL 검증 |
| 95 | `infra/.../member/dao/MemberQueryDaoWithdrawalTest.kt` | 신규 | findExpired/existsActiveOrPending |
| 96 | `boot/ma-boot-batch/.../MemberWithdrawalCompleteJobTest.kt` | 신규 | 8개 Step 순차 실행, 만료 회원만 익명화, 미만료는 보존 |
| 97 | `boot/ma-boot-batch/.../*ItemWriterTest.kt` (도메인별 8개) | 신규 | 각 Writer cleanup port 호출 검증 |

### Phase 12: 문서

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 98 | `docs/api-todo.md` | 수정 | "기타 > 회원 탈퇴" → 완료 섹션 이동 |
| 99 | `boot/.../src/docs/asciidoc/member-withdrawal.adoc` | 신규 | snippet include |
| 100 | `boot/.../src/docs/asciidoc/auth.adoc` | 수정 | withdrawal_pending 응답 |

---

## 6. 고려사항

### 6.1 심사 / 법적

- **Apple 5.1.1(v)** / **Google Play 데이터 안전 정책**: 두 가이드 모두 구체적 기간 명시 없음. "삭제됨" 결과만 요구. 7일 유예는 사용자 안내 문구만 명확하면 통과
- **즉시 처리 의무 없음**: 어느 가이드도 "신청 즉시 데이터 삭제"를 요구하지 않음. 7일 후 처리로 충분
- **GDPR Art.17**: 30일 이내 처리 의무 → 7일 유예 + 즉시 익명화는 충분
- **전자상거래법 시행령 §6**: 결제/거래 이력 5년 보존 → `POINT_HISTORIES`는 익명화만, soft delete X
- **개인정보보호법 §21**: "지체없이" 파기 → 7일 유예는 일반적으로 인정됨

### 6.2 RPW + 도메인별 Step — 함정과 대비

- **Step 단위 트랜잭션 (chunk 단위)**: 회원 단위 atomic은 깨짐. 한 chunk 안에서 일부 회원 실패 시 chunk 전체 롤백
- **멱등성**: 모든 cleanup 작업이 멱등이어야 함 — 같은 회원에 같은 cleanup이 두 번 호출돼도 결과 동일
  - `softDelete`: 이미 deleted=true여도 OK
  - `anonymizeAuthor`: 이미 익명화된 이메일에 다시 익명화 적용해도 결과 동일 (`withdrawn_{id}@deleted.local` 자기 자신)
  - `deleteByMember`: 없으면 no-op
- **부분 처리 시 노출 안전**: `Member.withdrawalRequestedAt`이 처음 set 후 익명화 후에도 보존됨 → 모든 즉시 숨김 필터/인터셉터가 부분 처리 상태에서도 작동
- **Step 순서 보장**: Spring Batch Job이 `.next()`로 순차 실행. `MemberAnonymizeStep`은 마지막 (다른 Step이 회원 조회 가능해야 하므로)
- **Reader 반복 조회**: 8개 Step이 같은 만료 회원 목록을 8번 조회. 인덱스 있어 부담 적지만, 성능 이슈 발견 시 JobExecutionContext 캐시 도입 고려
- **SkipPolicy**: 같은 회원이 반복 실패하면 skip 처리. 운영자 알림 필요

### 6.3 구현 / 기술

- **withdrawalRequestedAt 컬럼 + 인덱스**: 배치가 매일 만료 회원 조회 → `MEMBERS.WITHDRAWAL_REQUESTED_AT` 인덱스 필요
- **JWT 무효화 부재**: AccessToken은 stateless. `WithdrawalGuardInterceptor`로 액션 차단 → RefreshToken 즉시 삭제 불필요
- **MEMBER_POINTS.OWNER_EMAIL unique**: 익명화 시 ownerEmail까지 익명화 + softDelete로 충돌 회피
- **MEMBERS.email unique**: 익명화 이메일 형식이 unique 보장
- **유예 중 unique**: pending 회원의 원래 이메일/전화 유지 → 재가입 시 `existsActiveOrPending`으로 차단
- **이메일 정규식**: `Email`의 정규식이 `withdrawn_{id}@deleted.local`을 통과하는지 확인. 안 되면 정규식 완화 또는 `Email.withdrawn` 팩토리 검증 우회 (private 생성자)
- **소셜 로그인 분기점**: `MemberWithdrawalValidator`의 비번 검증 한 줄 (OCP)

### 6.4 운영

- **모니터링**: 일별 탈퇴 신청/복구/익명화 수 집계
- **배치 실패**: chunk 단위로 일부 실패해도 다음 batch 재시도. SkipPolicy로 반복 실패 격리
- **사용자 안내 문구** (frontend 책임):
  - 탈퇴 신청: "7일 뒤 영구 삭제됩니다. 그 안에 복구 가능"
  - 복구 화면: "탈퇴 진행 중입니다. 복구하시겠습니까?"
  - 재가입 차단: "탈퇴 진행 중인 계정이 있습니다"

### 6.5 1차 범위 외

- 사회적 로그인 분기 (`Member.loginType`)
- JWT blacklist (Redis)
- 탈퇴 사유 수집
- 좋아요 카운트 보정 배치
- X룸 블록/공유/차단
- 결제 환불 정책
- pending 상태 Redis 캐시
- 알림 발송 (탈퇴 완료 시 이메일/SMS)
- JobExecutionContext 캐시 (Reader 반복 조회 부담 발견 시)

---

## 7. 검증 항목

### 7.1 도메인 / 단위

- [ ] `Member.requestWithdrawal(now)` → `withdrawalRequestedAt = now`
- [ ] `Member.cancelWithdrawal()` → `withdrawalRequestedAt = null`
- [ ] `Member.isWithdrawalPending(now)` 7일 미만 true
- [ ] `Member.isWithdrawalExpired(now)` 7일 경과 true
- [ ] `Member.anonymize(memberId)` 모든 PII sentinel 값
- [ ] `Email.withdrawn(id)` 형식/정규식 통과

### 7.2 통합 — 신청 / 복구

- [ ] `DELETE /api/members/me` 성공 시 204 + `withdrawalRequestedAt` 세팅. 외부 도메인 부수 효과 없음 (RefreshToken은 그대로)
- [ ] 같은 회원 재 `DELETE` → 409
- [ ] `MemberWithdrawalService.requestWithdrawal` 단위 테스트 — Member update만 verify
- [ ] `POST /api/members/me/withdrawal/cancel` 200 + 새 토큰

### 7.3 통합 — 로그인 분기

- [ ] active 회원 → `status=ACTIVE`
- [ ] pending 회원 → `status=WITHDRAWAL_PENDING` + `withdrawalExpiresAt`

### 7.4 통합 — 액션 차단

- [ ] pending 회원 게시글 작성 → 403 (RefreshToken 살아있어도 인터셉터 차단)
- [ ] pending 회원 토큰 갱신 → 200 (갱신 자체는 됨), 새 토큰으로 액션 시도 → 403
- [ ] pending 회원 cancel API → 통과

### 7.5 통합 — 재가입 차단

- [ ] pending 회원 이메일/전화로 sign-up → `WithdrawalPendingMemberException`
- [ ] pending 회원 이메일로 `email/exists` → 사용 불가

### 7.6 통합 — 즉시 숨김

- [ ] pending 회원이 매칭 결과/Claim/TargetInfo에서 제외
- [ ] pending 회원 작성 게시글 작성자 닉네임이 "탈퇴한회원"으로 표시
- [ ] 본문/댓글 유지

### 7.7 배치 — RPW + 도메인별 Step

- [ ] `findExpiredWithdrawalRequests(now, pageable)` 페이징 동작
- [ ] 8개 Step 순차 실행 (`MemberAnonymizeStep` 마지막)
- [ ] 각 Step Writer가 자기 도메인 cleanup port 호출 (단위 테스트)
- [ ] 만료 회원만 익명화, 미만료는 보존
- [ ] chunk 단위 트랜잭션 — chunk 안 한 명 실패 시 chunk 롤백
- [ ] 같은 회원 재배치 시 멱등 (이미 처리된 데이터에 같은 cleanup 호출해도 결과 동일)
- [ ] `MEMBERS.email`, `MEMBER_POINTS.OWNER_EMAIL` unique 충돌 없음 (재가입 가능)
- [ ] `MATCHING_RESULTS` 양방향 softDelete
- [ ] `COMMUNITY_POSTS` 작성자 익명화 후 본문/likes 카운트 보존
- [ ] `POINT_HISTORIES` ownerEmail 익명화 후 deleted=false
- [ ] `POST_LIKES`/`COMMENT_LIKES` hard delete

### 7.8 회귀

- [ ] 기존 LoginApi/MemberPhotoApi/PostQueryApi 등 회귀 통과
- [ ] `./gradlew build` 성공

---

## 8. 후속 작업 (참고)

- 결제 환불 정책 (탈퇴 시 잔여 포인트)
- 탈퇴 사유 수집
- pending 상태 Redis 캐시 (인터셉터 부하)
- JobExecutionContext 캐시 (Reader 반복 조회 부담)
- JWT blacklist
- 좋아요 카운트 보정 배치
- 30일 통계 대시보드
- 사용자 알림
- 소셜 로그인 도입 시 분기

---

## 📋 스킬 적용 체크리스트

### plan-writing 스킬
- [x] 코드 스니펫 없이 시그니처/설명 수준
- [x] 변경 전략 테이블 정리
- [x] Phase별 변경 파일 목록 한 줄 요약
- [x] `docs/plan/202605/`에 저장
- [x] 고려사항(심사, FK, unique, 트랜잭션, 토큰, 유예 정책, RPW Step 함정) 포함

### code-implementation-rules 스킬
- [x] Service는 조합만 담당, 검증은 Validator로 분리
- [x] 도메인 객체에 행위 부여 (`Member.requestWithdrawal`, `cancelWithdrawal`, `isWithdrawalPending`, `anonymize`)
- [x] 포트 인터페이스가 도메인 객체 사용
- [x] FK 미사용
- [x] 메서드 네이밍 — 의도 드러내는 접미사
- [x] 새 도메인 분리 지양 — `member` 하위 application/policy 패키지로 처리
- [x] **도메인 의존 단방향 유지** — 도메인 모듈끼리 추가 의존 없음. cleanup은 batch 모듈에서 처리
- [x] 도메인 객체 변환은 도메인 모듈 내부
- [x] 하드코딩 회피 — sentinel 상수, `WithdrawalPolicy.GRACE_PERIOD_DAYS`
