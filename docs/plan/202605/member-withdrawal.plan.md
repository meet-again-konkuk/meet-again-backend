# Plan: 회원 탈퇴 (Account Deletion) — 7일 유예 + 도메인 이벤트 모델

> 작성일: 2026-05-03
> 상태: Draft
> 변경 이력:
> - 2026-05-03 즉시 익명화 → 7일 유예 + 배치 익명화 모델로 변경
> - 2026-05-06 도메인 간 의존(member ↔ auth/matching/point/community/support/xroom) 제거를 위해 **Spring 도메인 이벤트** 패턴 도입

## 1. 개요

앱스토어/구글플레이 심사를 위한 in-app account deletion API를 구현한다 (Apple Guideline 5.1.1(v), Google Play 데이터 안전 정책).

- **모델**: **7일 유예 기간 + 배치 익명화**
  - 사용자가 탈퇴 신청 시 즉시 다른 사용자에게 안 보이고, 본인 액션도 차단됨
  - 7일 동안 명시적 복구(취소) 가능
  - 7일 경과 시 배치가 PII 익명화 + 관련 데이터 정리
- **도메인 협력**: **Spring `ApplicationEventPublisher` + `@TransactionalEventListener(BEFORE_COMMIT)`**
  - `member` 도메인은 다른 도메인을 모름 — 이벤트만 발행
  - 각 도메인(`auth`, `matching`, `point`, `community`, `support`, `xroom`)이 자기 cleanup을 자기 핸들러에서 수행
  - 의존 방향: 모두 → `member` (단방향 유지)
  - 모든 핸들러가 같은 트랜잭션에서 실행되어 atomic 처리 보장
- **유예 기간 정책 (확정)**:
  - 유예 중 로그인 시도 → **명시적 복구** (특수 응답 → 클라이언트 복구 화면 → 복구 API 호출)
  - 유예 중 같은 이메일/전화번호로 재가입 → **차단** ("탈퇴 진행 중인 계정")
  - 유예 중 다른 사용자에게 노출 → **즉시 숨김** (매칭/게시글/댓글 등에서 사라짐)
  - 유예 중 본인 액션 (충전/매칭/글쓰기 등) → **모두 차단**
- **응답**: `204 No Content` (탈퇴 신청), `200` (복구)
- **상수**: `WithdrawalPolicy.GRACE_PERIOD_DAYS = 7`

### 1.1 도메인 분리

새 도메인을 만들지 않고 **기존 `member` 도메인 안의 application 서비스**(`MemberWithdrawalService`, `MemberWithdrawalCancelService`, `MemberAnonymizationService`)로 추가. 배치는 `ma-boot-batch`에 신규 잡 추가.

각 도메인의 cleanup 작업은 **자기 도메인 안의 EventHandler**가 담당:
- `auth/.../event/MemberWithdrawalRequestedEventHandler` — RefreshToken 삭제
- `matching/.../event/MemberAnonymizedEventHandler` — TargetInfo, MatchingResult cleanup
- `point/.../event/MemberAnonymizedEventHandler` — MemberPoint, PointHistory
- `community/.../event/MemberAnonymizedEventHandler` — Post, Comment, PostLike, CommentLike
- `support/.../event/MemberAnonymizedEventHandler` — Inquiry
- `xroom/.../event/MemberAnonymizedEventHandler` — Xroom

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

#### 동작 (단일 트랜잭션, BEFORE_COMMIT 핸들러 묶음)

1. 비밀번호 검증 (`MemberWithdrawalValidator`)
2. `Member.requestWithdrawal(now)` → save
3. `publishEvent(MemberWithdrawalRequestedEvent(member.email))`
4. → `auth.MemberWithdrawalRequestedEventHandler` 가 같은 트랜잭션에서 RefreshToken hard delete
5. **이 시점부터** 다른 사용자 화면에서 즉시 숨김, 본인 인증 시 액션 차단
6. 별도 cleanup 작업은 **하지 않음** — 7일 후 배치가 처리

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
1. `Member.cancelWithdrawal()` → save
2. `publishEvent(MemberWithdrawalCancelledEvent(email))` — 1차에서는 리스너 없음, 추후 알림용 hook
3. 새 access/refresh token 발급

> **로그인 흐름과의 관계**: `LoginService`가 `member.isWithdrawalPending(now)` 체크 → `LoginInfo.WithdrawalPending` 응답. 클라이언트는 응답 받아 복구 화면 → 사용자가 복구 버튼 누르면 cancel API 호출.

---

## 3. 삭제 전략 — 시점별 처리

### 3.1 즉시 처리 (탈퇴 신청 시점, 도메인 이벤트 흐름)

**최소한의 즉시 작업** — 다른 사용자 노출 차단과 토큰 무효화만.

| 단계 | 위치 | 처리 |
|---|---|---|
| 1 | `MemberWithdrawalService` | `Member.withdrawalRequestedAt` 컬럼 set (DB update) |
| 2 | `MemberWithdrawalService` | `publishEvent(MemberWithdrawalRequestedEvent)` |
| 3 | `auth.MemberWithdrawalRequestedEventHandler` (BEFORE_COMMIT) | RefreshToken hard delete |

> **노출 차단은 별도 데이터 변경 없이 조회 쿼리/응답 변환에서 처리** — §3.3 참조.

### 3.2 유예 후 처리 (배치, `MemberWithdrawalCompleteJob`)

7일 경과 회원에 대해 다음 흐름 (회원 단위 트랜잭션):

| 단계 | 위치 | 처리 |
|---|---|---|
| 1 | `MemberAnonymizationService.anonymize(member)` | `Email.withdrawn(member.id)`로 익명화 이메일 생성 |
| 2 | 동일 | `publishEvent(MemberAnonymizedEvent(oldEmail, newEmail, memberId))` |
| 3 | `auth.MemberAnonymizedEventHandler` (BEFORE_COMMIT) | RefreshToken hard delete (재실행, 이미 삭제됐을 수 있음 — idempotent) |
| 3 | `matching.MemberAnonymizedEventHandler` (BEFORE_COMMIT) | TargetInfo soft delete (`deleteByRegisterEmail`), MatchingResult 양방향 soft delete |
| 3 | `point.MemberAnonymizedEventHandler` (BEFORE_COMMIT) | MemberPoint(잔액 0 + 익명화 + softDelete), PointHistory(`anonymizeOwner`) |
| 3 | `community.MemberAnonymizedEventHandler` (BEFORE_COMMIT) | Post/Comment `anonymizeAuthor`, PostLike/CommentLike hard delete |
| 3 | `support.MemberAnonymizedEventHandler` (BEFORE_COMMIT) | Inquiry `anonymizeAuthor` |
| 3 | `xroom.MemberAnonymizedEventHandler` (BEFORE_COMMIT) | Xroom soft delete (`deleteByOwner`) |
| 3 | `member.MemberPhotoCleanupEventHandler` (BEFORE_COMMIT, **같은 도메인**) | MemberPhoto 파일 + DB 삭제 |
| 4 | `MemberAnonymizationService.anonymize` | (publishEvent 이후) `Member.anonymize(memberId)` → save (PII 익명화 + soft delete) |

> **순서 보장**: BEFORE_COMMIT 핸들러 간 순서는 무보장이지만 모두 **다른 테이블 update**라 무관. **Member 자체 익명화는 publishEvent 호출 후 직접 처리**(코드 흐름상 보장)되어 다른 핸들러가 익명화 이메일을 사용 가능.

### 3.3 도메인별 처리 매트릭스

| 도메인 | 테이블 | 식별 컬럼 | 처리 | 처리 위치 |
|--------|--------|-----------|------|----------|
| **member** | `MEMBERS` | `EMAIL` | 익명화 + soft delete | `MemberAnonymizationService` (직접) |
| **member** | `MEMBER_PHOTOS` | `MEMBER_EMAIL` | soft delete + 파일 삭제 | `MemberPhotoCleanupEventHandler` |
| **auth** | `REFRESH_TOKENS` | `EMAIL` | hard delete | `MemberWithdrawalRequestedEventHandler` (즉시), `MemberAnonymizedEventHandler` (배치, idempotent) |
| **matching** | `TARGET_INFOS` | `REGISTER_EMAIL` | soft delete | `matching.MemberAnonymizedEventHandler` |
| **matching** | `MATCHING_RESULTS` | `REGISTER_EMAIL`/`TARGET_EMAIL` | soft delete (양방향) | 동일 |
| **point** | `MEMBER_POINTS` | `OWNER_EMAIL` (unique) | 잔액 0 + 익명화 + soft delete | `point.MemberAnonymizedEventHandler` |
| **point** | `POINT_HISTORIES` | `OWNER_EMAIL` | 익명화 (이메일만) | 동일 |
| **community** | `COMMUNITY_POSTS` | `AUTHOR_EMAIL` | 익명화 (작성자만) | `community.MemberAnonymizedEventHandler` |
| **community** | `COMMUNITY_COMMENTS` | `AUTHOR_EMAIL` | 익명화 (작성자만) | 동일 |
| **community** | `COMMUNITY_POST_LIKES` | `MEMBER_EMAIL` | hard delete | 동일 |
| **community** | `COMMUNITY_COMMENT_LIKES` | `MEMBER_EMAIL` | hard delete | 동일 |
| **support** | `INQUIRIES` | `AUTHOR_EMAIL` | 익명화 | `support.MemberAnonymizedEventHandler` |
| **xroom** | `XROOMS` | `OWNER_EMAIL` | soft delete | `xroom.MemberAnonymizedEventHandler` |

### 3.4 즉시 숨김 처리 (유예 기간 동안)

| 데이터 종류 | 처리 방식 | 위치 |
|-------------|----------|------|
| **매칭 결과** | DB 쿼리 필터 — 상대방 Member의 `withdrawalRequestedAt IS NULL` 조건 추가 | `MatchingResultQueryDao` |
| **TargetInfo 후보** | DB 쿼리 필터 — register Member `withdrawalRequestedAt IS NULL` | `TargetInfoQueryDao` |
| **Claimer 목록** | DB 쿼리 필터 | `ClaimerQueryDao` |
| **게시글/댓글 작성자** | 응답 변환 — `WithdrawnNicknameMasker.mask(member)` | `Post`/`Comment` query 매퍼 |

> **선택 근거**: "보여줄지 말지" → DB 필터, "본문은 유지하되 작성자만 가리기" → 응답 변환. 두 패턴은 7일 후 익명화된 상태와 동일한 결과.

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

### 4.1 도메인 모델 / 정책 / 이벤트

| 클래스 | 변경 | 책임 |
|--------|------|------|
| `Member` | 수정 | `withdrawalRequestedAt: LocalDateTime?` 필드 추가. `requestWithdrawal(now)`, `cancelWithdrawal()`, `isWithdrawalPending(now)`, `isWithdrawalExpired(now)`, `isActive()`, `anonymize(memberId)` 메서드 |
| `WithdrawalPolicy` | 신규 | `GRACE_PERIOD_DAYS = 7L`, `expiresAt(requestedAt)` |
| `WithdrawnSentinel` | 신규 | 익명화 sentinel 상수 |
| `WithdrawnNicknameMasker` | 신규 | `mask(member): String` |
| `Email.withdrawn(memberId)` | 수정 | 팩토리 |
| `MemberWithdrawalRequestedEvent` | 신규 (`member.domain.event`) | `data class(email: Email)`. immutable |
| `MemberAnonymizedEvent` | 신규 | `data class(memberId: Long, oldEmail: Email, newEmail: Email)`. immutable |
| `MemberWithdrawalCancelledEvent` | 신규 | `data class(email: Email)`. immutable. 1차 리스너 없음 (hook) |
| `AlreadyWithdrawalRequestedException` | 신규 | 도메인 예외 |
| `NotWithdrawalRequestedException` | 신규 | 도메인 예외 |
| `WithdrawalPendingMemberException` | 신규 | 재가입 차단 |

### 4.2 application — 신청 / 복구 / 익명화

| 클래스 | 위치 | 책임 |
|--------|------|------|
| `WithdrawalRequestCommand` | `member.application.command` | `data class(email, password)` |
| `MemberWithdrawalValidator` | `member.domain` | `validate(member, password)` — 비번 + 이미 pending 차단 |
| `MemberWithdrawalCancelValidator` | `member.domain` | `validate(member)` — pending 상태 검증 |
| `MemberWithdrawalService` | `member.application` | `@Service @Transactional`. `requestWithdrawal(command)` — 검증 → Member 업데이트 → **publishEvent(MemberWithdrawalRequestedEvent)** |
| `MemberWithdrawalCancelService` | `member.application` | `@Service @Transactional`. `cancel(email): LoginInfo` — Member 업데이트 → publishEvent → 새 토큰 발급 |
| `MemberAnonymizationService` | `member.application` | `@Service @Transactional`. `anonymize(member)` — **publishEvent(MemberAnonymizedEvent)** → Member 자체 익명화 + softDelete. 배치/유예만료 케이스에서 호출 |

### 4.3 도메인 이벤트 핸들러 (도메인별 신규)

모두 `@Component` + `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)`.

| 핸들러 | 위치 | 처리 이벤트 | 책임 |
|--------|------|------------|------|
| `MemberWithdrawalRequestedEventHandler` | `auth.domain.event` | `MemberWithdrawalRequestedEvent` | RefreshToken hard delete |
| `MemberAnonymizedEventHandler` | `auth.domain.event` | `MemberAnonymizedEvent` | RefreshToken hard delete (idempotent) |
| `MemberAnonymizedEventHandler` | `matching.domain.event` | `MemberAnonymizedEvent` | TargetInfo `deleteByRegisterEmail`, MatchingResult `deleteByMember` |
| `MemberAnonymizedEventHandler` | `point.domain.event` | `MemberAnonymizedEvent` | MemberPoint `deleteByOwner`, PointHistory `anonymizeOwner` |
| `MemberAnonymizedEventHandler` | `community.domain.event` | `MemberAnonymizedEvent` | Post/Comment `anonymizeAuthor`, PostLike/CommentLike `deleteByMember` |
| `MemberAnonymizedEventHandler` | `support.domain.event` | `MemberAnonymizedEvent` | Inquiry `anonymizeAuthor` |
| `MemberAnonymizedEventHandler` | `xroom.domain.event` | `MemberAnonymizedEvent` | Xroom `deleteByOwner` |
| `MemberPhotoCleanupEventHandler` | `member.domain.event` | `MemberAnonymizedEvent` | MemberPhoto 파일 삭제 + DB row 삭제 (`MemberPhotoCleaner` 위임) |

> **member 도메인은 다른 도메인을 알지 못함**. 의존 방향: `auth/matching/point/community/support/xroom` → `member` (이벤트 클래스 import). 단방향 유지.

### 4.4 로그인 흐름 변경

| 클래스 | 변경 | 내용 |
|--------|------|------|
| `LoginInfo` | 수정 (sealed) | `LoginInfo.Active(...)`, `LoginInfo.WithdrawalPending(accessToken, refreshToken, withdrawalRequestedAt, withdrawalExpiresAt)` |
| `LoginService` | 수정 | 비번 검증 후 `member.isWithdrawalPending(now)` 분기 |
| `LoginApi` | 수정 | sealed 분기에 따라 응답 매핑. `status: "ACTIVE" \| "WITHDRAWAL_PENDING"` 필드 |

### 4.5 액션 차단 (유예 중)

| 클래스 | 변경 | 내용 |
|--------|------|------|
| `WithdrawalGuardInterceptor` | 신규 | `HandlerInterceptor`. 인증된 사용자의 `Member.withdrawalRequestedAt` 체크 → not null이면 403. `@SkipWithdrawalGuard`는 통과 |
| `@SkipWithdrawalGuard` | 신규 | annotation. cancel 엔드포인트 부착 |
| `WebMvcConfig` | 수정 | 인터셉터 등록 |

> **성능**: 매 요청마다 Member 조회 부담 → 1차는 DB 조회. 부하 발견 시 Redis 캐시(`member:withdrawal:{email}`) 도입 검토.

### 4.6 재가입 차단

| 클래스 | 변경 | 내용 |
|--------|------|------|
| `SignUpValidator` | 수정 | 이메일/전화번호 unique 검증 시 pending 회원 포함. 발견 시 `WithdrawalPendingMemberException` |
| `MemberQueryRepository.existsActiveOrPendingByEmail` | 신규 메서드 | pending 포함 |
| `MemberQueryRepository.existsActiveOrPendingByPhoneNumber` | 신규 메서드 | 동일 |
| 이메일/닉네임 중복 확인 API | 수정 | 동일 정책 |

### 4.7 즉시 숨김 — 조회 필터/응답 변환

| 위치 | 변경 |
|------|------|
| `MatchingResultQueryDao.findResults` | 상대방 Member `withdrawalRequestedAt IS NULL` 조건 |
| `TargetInfoQueryDao.findCandidates` | register Member 동일 |
| `ClaimerQueryDao.findMyClaimers` | claimer Member 동일 |
| `Post`/`Comment` query 응답 매퍼 | `WithdrawnNicknameMasker.mask(member)` 호출 |

### 4.8 배치 — 7일 후 익명화 (Chunk-oriented)

Spring Batch 정석인 **Reader / Writer 패턴** 사용 (Processor는 변환 작업이 없어 생략 또는 PassThrough).

| 클래스 | 위치 | 책임 |
|--------|------|------|
| `MemberWithdrawalCompleteJobConfig` | `boot/ma-boot-batch/.../job/domain/member` | Job + Step 설정. **chunk size = 1**. cron `0 0 3 * * *` |
| `ExpiredWithdrawalMemberItemReader` | 동일 | `ItemReader<Member>`. 페이지 단위로 만료 회원 stream. 내부적으로 `MemberQueryRepository.findExpiredWithdrawalRequests(now, pageable)` 사용 |
| `MemberAnonymizationItemWriter` | 동일 | `ItemWriter<Member>`. chunk(`List<Member>`) 받아 각 회원별 `MemberAnonymizationService.anonymize(member)` 호출 |
| `MemberQueryRepository.findExpiredWithdrawalRequests(now, pageable)` | port | 신규 메서드. 페이징 지원 |

**chunk size = 1 결정 이유**:
- 한 회원의 익명화는 단일 트랜잭션 atomic이 되어야 함 (도메인 이벤트 BEFORE_COMMIT)
- chunk size > 1 시 chunk 안에 N개 회원이 한 트랜잭션 → 한 회원 실패 시 N개 모두 롤백
- chunk size = 1로 회원 단위 트랜잭션 보장
- `SkipPolicy` 또는 `FaultTolerantStepBuilder.skip` 설정으로 실패 회원 스킵, 나머지 진행

**Reader 페이징 전략**:
- 한 번에 모든 만료 회원을 메모리에 올리지 않음 (page size = 100 권장)
- `RepositoryItemReader` 또는 커스텀 Reader로 `findExpiredWithdrawalRequests(now, PageRequest.of(page, 100))` 호출
- 정렬은 `memberId asc` (페이징 일관성)

**Processor 사용 안 함**:
- 회원 탈퇴는 데이터 변환이 아니라 부수효과(이벤트 발행 → 도메인 cleanup) 발생
- Processor에 부수효과 넣는 건 Spring Batch 정석이 아님 → Writer에 응집
- 향후 필터링 필요 시 추가 (예: 특정 조건 회원 제외)

**트랜잭션 안에서 publishEvent** → BEFORE_COMMIT 핸들러가 같은 트랜잭션에서 실행되어 atomic.

### 4.9 트랜잭션 / 동시성

- **탈퇴 신청**: 단일 `@Transactional`. Member update + publishEvent → BEFORE_COMMIT 핸들러(RefreshToken delete)가 같은 트랜잭션에서 실행 → 커밋
- **복구**: 단일 `@Transactional`. Member update + publishEvent + 새 토큰 발급
- **배치 익명화**: 회원 단위 `@Transactional`. publishEvent → 6개 도메인 핸들러 + Member 자체 익명화 → 커밋
- **이벤트 트랜잭션 보장**: 모든 publishEvent는 `@Transactional` 메서드 안에서 호출 → BEFORE_COMMIT 핸들러 정상 실행
- **순서 무관**: 각 도메인 핸들러가 다른 테이블을 update → deadlock 위험 미미
- **Member 자체 익명화는 publishEvent 후 직접 처리** → 다른 핸들러가 익명화 이메일을 사용 가능 (이벤트 payload로 전달)

### 4.10 토큰 무효화

- **RefreshToken**: 탈퇴 신청 시 `auth.MemberWithdrawalRequestedEventHandler`가 즉시 hard delete
- **AccessToken**: 만료까지 유효하지만 `WithdrawalGuardInterceptor`가 모든 액션 차단 → 사실상 무력화
- **복구 시**: 새 access + refresh token 발급

---

## 5. 변경 파일 목록

### Phase 1: 도메인 모델 / 정책 / 이벤트

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 1 | `domain/.../member/domain/Member.kt` | 수정 | `withdrawalRequestedAt: LocalDateTime?` + 메서드 추가 |
| 2 | `domain/.../member/domain/policy/WithdrawalPolicy.kt` | 신규 | object. `GRACE_PERIOD_DAYS = 7L`, `expiresAt(requestedAt)` |
| 3 | `domain/.../member/domain/policy/WithdrawnSentinel.kt` | 신규 | sentinel 상수 |
| 4 | `domain/.../member/domain/WithdrawnNicknameMasker.kt` | 신규 | `mask(member: Member): String` |
| 5 | `domain/.../common/domain/Email.kt` | 수정 | `companion`에 `withdrawn(memberId): Email` |
| 6 | `domain/.../member/domain/event/MemberWithdrawalRequestedEvent.kt` | 신규 | `data class(email: Email)` |
| 7 | `domain/.../member/domain/event/MemberAnonymizedEvent.kt` | 신규 | `data class(memberId: Long, oldEmail: Email, newEmail: Email)` |
| 8 | `domain/.../member/domain/event/MemberWithdrawalCancelledEvent.kt` | 신규 | `data class(email: Email)` |
| 9 | `domain/.../member/domain/exception/AlreadyWithdrawalRequestedException.kt` | 신규 | 예외 |
| 10 | `domain/.../member/domain/exception/NotWithdrawalRequestedException.kt` | 신규 | 예외 |
| 11 | `domain/.../member/domain/exception/WithdrawalPendingMemberException.kt` | 신규 | 예외 |

### Phase 2: 도메인 서비스 — 신청 / 복구 / 익명화

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 12 | `domain/.../member/domain/MemberWithdrawalValidator.kt` | 신규 | `validate(member, password)` |
| 13 | `domain/.../member/domain/MemberWithdrawalCancelValidator.kt` | 신규 | `validate(member)` |
| 14 | `domain/.../member/application/command/WithdrawalRequestCommand.kt` | 신규 | `data class(email, password)` |
| 15 | `domain/.../member/application/MemberWithdrawalService.kt` | 신규 | 검증 → Member update → `publishEvent(MemberWithdrawalRequestedEvent)` |
| 16 | `domain/.../member/application/MemberWithdrawalCancelService.kt` | 신규 | 검증 → Member update → publishEvent → 새 토큰 |
| 17 | `domain/.../member/application/MemberAnonymizationService.kt` | 신규 | publishEvent(MemberAnonymizedEvent) → Member.anonymize → save |
| 18 | `domain/.../member/domain/photo/MemberPhotoCleaner.kt` | 신규 | 사진 파일+DB 삭제 로직 추출 |
| 19 | `domain/.../member/application/MemberPhotoService.kt` | 수정 | 내부 로직을 `MemberPhotoCleaner` 위임 |

### Phase 3: 도메인 이벤트 핸들러

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 20 | `domain/.../member/domain/event/MemberPhotoCleanupEventHandler.kt` | 신규 | `MemberAnonymizedEvent` BEFORE_COMMIT → `MemberPhotoCleaner.clean(oldEmail)` |
| 21 | `domain/.../auth/domain/event/MemberWithdrawalRequestedEventHandler.kt` | 신규 | RefreshToken hard delete |
| 22 | `domain/.../auth/domain/event/MemberAnonymizedEventHandler.kt` | 신규 | RefreshToken hard delete (idempotent) |
| 23 | `domain/.../matching/domain/event/MemberAnonymizedEventHandler.kt` | 신규 | TargetInfo + MatchingResult cleanup |
| 24 | `domain/.../point/domain/event/MemberAnonymizedEventHandler.kt` | 신규 | MemberPoint + PointHistory cleanup |
| 25 | `domain/.../community/domain/event/MemberAnonymizedEventHandler.kt` | 신규 | Post + Comment + PostLike + CommentLike cleanup |
| 26 | `domain/.../support/domain/event/MemberAnonymizedEventHandler.kt` | 신규 | Inquiry cleanup |
| 27 | `domain/.../xroom/domain/event/MemberAnonymizedEventHandler.kt` | 신규 | Xroom cleanup |

### Phase 4: 로그인 / 인증 흐름

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 28 | `domain/.../auth/domain/LoginInfo.kt` | 수정 | sealed로. `Active`, `WithdrawalPending` |
| 29 | `domain/.../auth/application/LoginService.kt` | 수정 | pending 분기 |

### Phase 5: 도메인 포트 (cleanup용)

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 30 | `domain/.../member/domain/port/MemberCommandRepository.kt` | 수정 | `requestWithdrawal(member, requestedAt)` / `cancelWithdrawal(member)` / `anonymizeAndSoftDelete(member)` |
| 31 | `domain/.../member/domain/port/MemberQueryRepository.kt` | 수정 | `findExpiredWithdrawalRequests(now)`, `existsActiveOrPendingByEmail`, `existsActiveOrPendingByPhoneNumber` |
| 32 | `domain/.../matching/domain/port/TargetInfoCommandRepository.kt` | 수정 | `deleteByRegisterEmail(email)` |
| 33 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `deleteByMember(email)` 양방향 |
| 34 | `domain/.../point/domain/port/MemberPointRepository.kt` | 수정 | `deleteByOwner(email)` |
| 35 | `domain/.../point/domain/port/PointHistoryRepository.kt` | 수정 | `anonymizeOwner(oldEmail, newEmail)` |
| 36 | `domain/.../community/domain/port/PostCommandRepository.kt` | 수정 | `anonymizeAuthor(oldEmail, newEmail)` |
| 37 | `domain/.../community/domain/port/CommentCommandRepository.kt` | 수정 | `anonymizeAuthor(oldEmail, newEmail)` |
| 38 | `domain/.../community/domain/port/PostLikeRepository.kt` | 수정 | `deleteByMember(email)` (hard) |
| 39 | `domain/.../community/domain/port/CommentLikeRepository.kt` | 수정 | `deleteByMember(email)` (hard) |
| 40 | `domain/.../support/domain/port/InquiryCommandRepository.kt` | 수정 | `anonymizeAuthor(oldEmail, newEmail)` |
| 41 | `domain/.../xroom/domain/port/XroomCommandRepository.kt` | 수정 | `deleteByOwner(email)` |

### Phase 6: 즉시 숨김 — 쿼리/응답

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 42 | `infra/.../matching/dao/MatchingResultQueryDao.kt` | 수정 | 상대방 Member `withdrawalRequestedAt IS NULL` 조건 |
| 43 | `infra/.../matching/dao/TargetInfoQueryDao.kt` | 수정 | register Member 동일 |
| 44 | `infra/.../matching/dao/ClaimerQueryDao.kt` | 수정 | claimer Member 동일 |
| 45 | `boot/.../community/api/(Post\|Comment) 응답 매퍼` | 수정 | `WithdrawnNicknameMasker.mask` 호출 |

### Phase 7: 액션 차단 / 재가입 차단

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 46 | `boot/.../config/security/WithdrawalGuardInterceptor.kt` | 신규 | `HandlerInterceptor` |
| 47 | `boot/.../config/security/SkipWithdrawalGuard.kt` | 신규 | annotation |
| 48 | `boot/.../config/WebMvcConfig.kt` | 수정 | 인터셉터 등록 |
| 49 | `domain/.../auth/domain/SignUpValidator.kt` | 수정 | pending 포함 unique 검증 |
| 50 | `domain/.../member/application/MemberQueryService.kt` | 수정 | 이메일/닉네임/전화 중복 확인 동일 정책 |

### Phase 8: 인프라 — Table / DAO

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 51 | `infra/.../member/entity/table/MemberTable.kt` | 수정 | `WITHDRAWAL_REQUESTED_AT` datetime nullable + 인덱스 |
| 52 | `infra/.../member/entity/MemberEntity.kt` | 수정 | 매핑 |
| 53 | `infra/.../member/dao/MemberCommandDao.kt` | 수정 | `requestWithdrawal`, `cancelWithdrawal`, `anonymizeAndSoftDelete` |
| 54 | `infra/.../member/dao/MemberQueryDao.kt` | 수정 | `findExpiredWithdrawalRequests`, `existsActiveOrPendingByEmail`, `existsActiveOrPendingByPhoneNumber` |
| 55 | `infra/.../matching/dao/TargetInfoCommandDao.kt` | 수정 | `deleteByRegisterEmail` |
| 56 | `infra/.../matching/dao/MatchingResultCommandDao.kt` | 수정 | `deleteByMember` 양방향 |
| 57 | `infra/.../point/dao/MemberPointCommandDao.kt` | 수정/신규 | `deleteByOwner` |
| 58 | `infra/.../point/dao/PointHistoryDao.kt` | 수정 | `anonymizeOwner` |
| 59 | `infra/.../community/dao/PostCommandDao.kt` | 수정 | `anonymizeAuthor` |
| 60 | `infra/.../community/dao/CommentCommandDao.kt` | 수정 | `anonymizeAuthor` |
| 61 | `infra/.../community/dao/PostLikeDao.kt` | 수정 | `deleteByMember` (hard) |
| 62 | `infra/.../community/dao/CommentLikeDao.kt` | 수정 | `deleteByMember` (hard) |
| 63 | `infra/.../support/dao/InquiryCommandDao.kt` | 수정 | `anonymizeAuthor` |
| 64 | `infra/.../xroom/dao/XroomCommandDao.kt` | 수정 | `deleteByOwner` |

### Phase 9: 인프라 — Repository

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 65 | `infra/.../member/repository/MemberCommandCoreRepository.kt` | 수정 | 신규 메서드 구현 |
| 66 | `infra/.../member/repository/MemberQueryCoreRepository.kt` | 수정 | 신규 메서드 구현 |
| 67~75 | 각 도메인 `*CoreRepository.kt` | 수정 | 신규 포트 메서드 DAO 위임 (9개) |

### Phase 10: Boot — Web API

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 76 | `boot/.../member/api/request/WithdrawalRequest.kt` | 신규 | `data class(password: String)` + `@field:NotBlank` |
| 77 | `boot/.../member/api/response/WithdrawalCancelResponse.kt` | 신규 | email, nickname, cancelledAt, accessToken, refreshToken |
| 78 | `boot/.../member/api/MemberWithdrawalApi.kt` | 신규 | DELETE/cancel. cancel은 `@SkipWithdrawalGuard` |
| 79 | `boot/.../auth/api/response/LoginResponse.kt` | 수정 | `status` 필드, `withdrawalRequestedAt`, `withdrawalExpiresAt` nullable |
| 80 | `boot/.../auth/api/LoginApi.kt` | 수정 | sealed 분기 매핑 |

### Phase 11: Boot — Batch (Chunk-oriented)

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 81 | `boot/ma-boot-batch/.../job/domain/member/MemberWithdrawalCompleteJobConfig.kt` | 신규 | Job + Step 설정. **chunk size = 1**. SkipPolicy 적용. cron `0 0 3 * * *` |
| 82 | `boot/ma-boot-batch/.../job/domain/member/ExpiredWithdrawalMemberItemReader.kt` | 신규 | `ItemReader<Member>`. 페이징(100). `findExpiredWithdrawalRequests(now, pageable)` |
| 83 | `boot/ma-boot-batch/.../job/domain/member/MemberAnonymizationItemWriter.kt` | 신규 | `ItemWriter<Member>`. 회원별 `MemberAnonymizationService.anonymize(member)` |
| 84 | `boot/ma-boot-batch/.../job/domain/member/MemberWithdrawalCompleteJobScheduler.kt` (선택) | 신규 | 스케줄러 트리거 |

### Phase 12: 테스트

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 84 | `domain/.../member/domain/MemberWithdrawalTest.kt` | 신규 | requestWithdrawal/cancelWithdrawal/isWithdrawalPending/isWithdrawalExpired/anonymize |
| 85 | `domain/.../member/domain/policy/WithdrawnSentinelTest.kt` | 신규 | sentinel 형식 |
| 86 | `domain/.../common/domain/EmailWithdrawnTest.kt` | 신규 | 형식/정규식 통과 |
| 87 | `domain/.../member/domain/MemberWithdrawalValidatorTest.kt` | 신규 | 비번 일치/불일치/이미 pending |
| 88 | `domain/.../member/domain/MemberWithdrawalCancelValidatorTest.kt` | 신규 | active/pending/expired |
| 89 | `domain/.../member/application/MemberWithdrawalServiceTest.kt` | 신규 | publishEvent 검증 (`ApplicationEventPublisher` Mockk verify) |
| 90 | `domain/.../member/application/MemberWithdrawalCancelServiceTest.kt` | 신규 | 동일 |
| 91 | `domain/.../member/application/MemberAnonymizationServiceTest.kt` | 신규 | publishEvent + Member 익명화 순서 |
| 92 | `domain/.../auth/domain/event/MemberWithdrawalRequestedEventHandlerTest.kt` | 신규 | RefreshToken 삭제 |
| 93 | `domain/.../*/domain/event/MemberAnonymizedEventHandlerTest.kt` (도메인별 6개) | 신규 | 각 도메인 cleanup verify |
| 94 | `domain/.../auth/application/LoginServiceTest.kt` | 수정 | active/withdrawal_pending 분기 |
| 95 | `domain/.../auth/domain/SignUpValidatorTest.kt` | 수정 | pending 회원 재가입 차단 |
| 96 | `boot/.../member/api/MemberWithdrawalApiTest.kt` | 신규 | REST Docs (DELETE 204, POST cancel 200, 401, 409, 400) |
| 97 | `boot/.../auth/api/LoginApiTest.kt` | 수정 | withdrawal_pending 응답 REST Docs |
| 98 | `boot/.../config/security/WithdrawalGuardInterceptorTest.kt` | 신규 | 차단/통과 |
| 99 | `infra/.../member/dao/MemberCommandDaoWithdrawalTest.kt` | 신규 | DDL 검증 |
| 100 | `infra/.../member/dao/MemberQueryDaoWithdrawalTest.kt` | 신규 | findExpired/existsActiveOrPending |
| 101 | `boot/ma-boot-batch/.../MemberWithdrawalCompleteJobTest.kt` | 신규 | end-to-end (이벤트 핸들러 통합) |
| 102 | `boot/ma-boot-web/.../MemberWithdrawalEventIntegrationTest.kt` | 신규 | 단일 트랜잭션 atomic 검증, 핸들러 실패 시 전체 롤백 |

### Phase 13: 문서

| # | 파일 | 변경 | 내용 |
|---|------|------|------|
| 103 | `docs/api-todo.md` | 수정 | "기타 > 회원 탈퇴" → 완료 섹션 이동 |
| 104 | `boot/.../src/docs/asciidoc/member-withdrawal.adoc` | 신규 | snippet include |
| 105 | `boot/.../src/docs/asciidoc/auth.adoc` | 수정 | withdrawal_pending 응답 |

---

## 6. 고려사항

### 6.1 심사 / 법적

- **Apple 5.1.1(v)** / **Google Play 데이터 안전 정책**: 두 가이드 모두 구체적 기간 명시 없음. "삭제됨" 결과만 요구. 7일 유예는 사용자 안내 문구만 명확하면 통과
- **GDPR Art.17**: 30일 이내 처리 의무 → 7일 유예 + 즉시 익명화는 충분
- **전자상거래법 시행령 §6**: 결제/거래 이력 5년 보존 → `POINT_HISTORIES`는 익명화만, soft delete X
- **개인정보보호법 §21**: "지체없이" 파기 → 7일 유예는 일반적으로 인정됨

### 6.2 도메인 이벤트 — 함정과 대비

- **Phase는 반드시 `BEFORE_COMMIT`**: 같은 트랜잭션·같은 connection 보장. `AFTER_COMMIT` 사용 시 부분 실패 일관성 깨짐
- **트랜잭션 컨텍스트 보장**: publishEvent 호출하는 메서드가 반드시 `@Transactional`이어야 함. 트랜잭션 밖에서 호출하면 BEFORE_COMMIT 핸들러는 소리 없이 미실행 → `MemberWithdrawalService` / `MemberAnonymizationService` 모두 클래스 또는 메서드 레벨 `@Transactional` 명시
- **이벤트 객체 immutable**: 모두 `data class` + `val`만 사용. 핸들러가 mutate하지 않도록
- **순서 무관**: 핸들러 간 순서는 보장 안 됨 — 우리 케이스는 모두 다른 테이블 update라 무관
- **Member 자체 익명화는 publishEvent 후 직접 처리**: 다른 핸들러가 익명화 이메일을 사용 가능해야 하므로 `MemberAnonymizationService` 흐름은 [익명화 이메일 생성 → publishEvent → Member.anonymize → save] 순서. 코드 흐름상 보장됨
- **idempotency**: `auth.MemberAnonymizedEventHandler`는 `auth.MemberWithdrawalRequestedEventHandler`가 이미 RefreshToken을 삭제했어도 정상 동작해야 함 (delete가 idempotent)
- **테스트**: 단위 테스트는 `ApplicationEventPublisher` Mockk verify. 통합 테스트는 `@RecordApplicationEvents` 또는 실제 SpringContext에서 end-to-end 시나리오 검증
- **연쇄 이벤트 금지**: 핸들러 안에서 또 다른 이벤트 발행 안 함 (디버깅 난이도 폭증)
- **DI 순환 회피**: 이벤트 클래스는 `member.domain.event` 패키지에. 다른 도메인이 import하지만 member는 다른 도메인의 어떤 클래스도 import하지 않음

### 6.3 구현 / 기술

- **withdrawalRequestedAt 컬럼 + 인덱스**: 배치가 매일 만료 회원을 조회하므로 인덱스 필요. `MEMBERS.WITHDRAWAL_REQUESTED_AT` 단일 인덱스
- **JWT 무효화 부재**: AccessToken은 stateless. `WithdrawalGuardInterceptor`로 액션 차단
- **임시 토큰 분리 안 함**: 일반 access token + 인터셉터에서 cancel만 통과
- **MEMBER_POINTS.OWNER_EMAIL unique**: 익명화 시 ownerEmail까지 익명화 + softDelete로 충돌 회피
- **MEMBERS.email unique**: 익명화 이메일 형식이 unique 보장
- **유예 중 unique**: pending 회원의 원래 이메일/전화 유지 → 재가입 시 `existsActiveOrPending`으로 차단
- **이메일 정규식**: `Email`의 정규식이 `withdrawn_{id}@deleted.local`을 통과하는지 확인. 안 되면 정규식 완화 또는 `Email.withdrawn` 팩토리 검증 우회 (private 생성자)
- **소셜 로그인 분기점**: `MemberWithdrawalValidator`의 비번 검증 한 줄 (OCP)

### 6.4 운영

- **모니터링**: 일별 탈퇴 신청/복구/익명화 수 집계
- **배치 실패**: 회원별 트랜잭션이라 일부 실패해도 다른 회원 처리. 실패 회원은 다음날 재시도
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
- 알림 발송 (탈퇴 완료 시 이메일/SMS) — 이벤트 추가 리스너로 가능

---

## 7. 검증 항목

### 7.1 도메인 / 단위

- [ ] `Member.requestWithdrawal(now)` → `withdrawalRequestedAt = now`
- [ ] `Member.cancelWithdrawal()` → `withdrawalRequestedAt = null`
- [ ] `Member.isWithdrawalPending(now)` 7일 미만 true
- [ ] `Member.isWithdrawalExpired(now)` 7일 경과 true
- [ ] `Member.anonymize(memberId)` 모든 PII sentinel 값
- [ ] `Email.withdrawn(id)` 형식/정규식 통과
- [ ] 이벤트 클래스 immutable (`data class` + `val`)

### 7.2 통합 — 신청 / 복구 (이벤트 흐름)

- [ ] `DELETE /api/members/me` 성공 시 204 + `withdrawalRequestedAt` 세팅 + RefreshToken 삭제 (이벤트 핸들러 실행 확인)
- [ ] 같은 회원 재 `DELETE` → 409
- [ ] `MemberWithdrawalService.requestWithdrawal` 단위 테스트 — `ApplicationEventPublisher.publishEvent` verify
- [ ] `auth.MemberWithdrawalRequestedEventHandler` 단위 테스트 — RefreshToken 삭제 검증
- [ ] `POST /api/members/me/withdrawal/cancel` 200 + 새 토큰

### 7.3 통합 — 로그인 분기

- [ ] active 회원 → `status=ACTIVE`
- [ ] pending 회원 → `status=WITHDRAWAL_PENDING` + `withdrawalExpiresAt`

### 7.4 통합 — 액션 차단

- [ ] pending 회원 게시글 작성 → 403
- [ ] pending 회원 cancel API → 통과

### 7.5 통합 — 재가입 차단

- [ ] pending 회원 이메일/전화로 sign-up → `WithdrawalPendingMemberException`
- [ ] pending 회원 이메일로 `email/exists` → 사용 불가

### 7.6 통합 — 즉시 숨김

- [ ] pending 회원이 매칭 결과/Claim/TargetInfo에서 제외
- [ ] pending 회원 작성 게시글 작성자 닉네임이 "탈퇴한회원"으로 표시
- [ ] 본문/댓글 유지

### 7.7 통합 — 배치 익명화 (Reader/Writer + 이벤트)

- [ ] `findExpiredWithdrawalRequests(now, pageable)` — 7일 미만 미포함, 7일 이상만 페이지 단위 반환
- [ ] `ExpiredWithdrawalMemberItemReader` 페이징 정상 동작 (1만 명 시뮬레이션, 메모리 부담 없음)
- [ ] **chunk size = 1**로 회원 단위 트랜잭션 보장 — 한 회원 실패 시 SkipPolicy로 스킵, 다른 회원 처리 계속
- [ ] `MemberAnonymizationService.anonymize` 단일 트랜잭션 안에서 모든 도메인 cleanup 실행 (실제 SpringContext 통합 테스트)
- [ ] 한 핸들러 실패 시 해당 회원 전체 롤백 — Member 익명화 + 다른 도메인 cleanup 모두 미적용. 다른 회원은 정상 처리
- [ ] 모든 핸들러 성공 시 13개 테이블 매트릭스대로 처리됨
- [ ] `MEMBERS.email`, `MEMBER_POINTS.OWNER_EMAIL` unique 충돌 없음 (재가입 가능)
- [ ] `MATCHING_RESULTS` 양방향 softDelete
- [ ] `COMMUNITY_POSTS` 작성자 익명화 후 본문/likes 카운트 보존
- [ ] `POINT_HISTORIES` ownerEmail 익명화 후 deleted=false
- [ ] `POST_LIKES`/`COMMENT_LIKES` hard delete
- [ ] RefreshToken idempotent (이미 삭제돼도 정상)
- [ ] 실패 회원은 다음 날 배치에서 재시도 (`withdrawalRequestedAt`이 그대로라 만료 조건 충족)

### 7.8 회귀

- [ ] 기존 LoginApi/MemberPhotoApi/PostQueryApi 등 회귀 통과
- [ ] `./gradlew build` 성공

---

## 8. 후속 작업 (참고)

- 결제 환불 정책 (탈퇴 시 잔여 포인트)
- 탈퇴 사유 수집
- pending 상태 Redis 캐시
- JWT blacklist
- 좋아요 카운트 보정 배치
- 30일 통계 대시보드
- 사용자 알림 (이벤트 리스너로 추가)
- 소셜 로그인 도입 시 분기

---

## 📋 스킬 적용 체크리스트

### plan-writing 스킬
- [x] 코드 스니펫 없이 시그니처/설명 수준
- [x] 변경 전략 테이블 정리
- [x] Phase별 변경 파일 목록 한 줄 요약
- [x] `docs/plan/202605/`에 저장
- [x] 고려사항(심사, FK, unique, 트랜잭션, 토큰, 유예 정책, 도메인 이벤트 함정) 포함

### code-implementation-rules 스킬
- [x] Service는 조합만 담당, 검증은 Validator로 분리
- [x] 도메인 객체에 행위 부여
- [x] 포트 인터페이스가 도메인 객체 사용
- [x] FK 미사용
- [x] 메서드 네이밍 — 의도 드러내는 접미사
- [x] 새 도메인 분리 지양 — `member` 하위 application/policy/event 패키지로 처리
- [x] **도메인 의존 단방향 유지** — Spring 도메인 이벤트로 member ↔ 다른 도메인 양방향 의존 회피
- [x] 도메인 객체 변환은 도메인 모듈 내부
- [x] 하드코딩 회피 — sentinel 상수, `WithdrawalPolicy.GRACE_PERIOD_DAYS`
