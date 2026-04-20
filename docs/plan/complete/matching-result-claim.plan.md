# Design: 매칭 결과 Claim (당신이 나의 X임을 표시하기)

> 작성일: 2026-04-16

## 1. 설계 개요

MATCHING_RESULTS 테이블에 `CLAIMED` boolean 컬럼을 추가하고, 매칭 결과의 등록자(A)가 "상대방(B)이 나의 X"라고 claim하는 기능을 구현한다. B는 자신을 claim한 목록을 조회할 수 있다. 기존 matching 도메인에 필드와 행위를 추가하는 방식으로 처리한다.

---

## 2. API 설계

api-todo.md의 "상대방에게 당신이 나의 X임을 표시하기" 항목에 해당한다.

### 2.1 Claim API

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| PATCH | `/api/matching-results/{matchingResultId}/claim` | 매칭 결과에 대해 claim 표시 | 필요 |

- 기존 exclude/include 패턴과 동일한 PATCH + 하위 리소스 방식
- 요청 본문 없음 (claimed = true로 변경)

### 2.2 나를 Claim한 목록 조회 API

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| GET | `/api/matching-results/claimed-by` | 나를 X로 지목한 사람 목록 조회 | 필요 |

- B(targetEmail)가 자신을 claim한 결과 목록을 조회
- 조건: `targetEmail = B AND claimed = true AND deleted = false`
- 응답 형태: 기존 MatchingResultsResponse 구조 재활용 (프로필 포함)

---

## 3. 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                            │
│  MatchingResultCommandApi                                   │
│    └── PATCH /{id}/claim → commandService.claim()           │
│  MatchingResultQueryApi                                     │
│    └── GET /claimed-by → queryService.findClaimedBy()       │
└────────────────────────────┬────────────────────────────────┘
                             │ (port)
┌────────────────────────────▼────────────────────────────────┐
│ domain/ma-domain-core                                       │
│  MatchingResultRepository (port)                            │
│    + updateClaimed(matchingResult: MatchingResult)           │
│    + findClaimedByTarget(email: Email): List<MatchingResult> │
│  MatchingResultCommandService                               │
│    + claim(matchingResultId: Long, email: String)            │
│  MatchingResultQueryService                                 │
│    + findClaimedBy(email: String): MatchingResultsWithProfiles │
└────────────────────────────┬────────────────────────────────┘
                             │ (implements)
┌────────────────────────────▼────────────────────────────────┐
│ infrastructure/storage/ma-db-core                           │
│  MatchingResultTable: + claimed = bool("CLAIMED")           │
│  MatchingResultEntity: + claimed 필드, toDomain()/from() 반영│
│  MatchingResultCommandDao                                   │
│    + updateClaimed(matchingResult: MatchingResult)           │
│  MatchingResultQueryDao                                     │
│    + findClaimedByTarget(email: String): List<MatchingResultEntity> │
│  MatchingResultCoreRepository                               │
│    + updateClaimed(), findClaimedByTarget() 구현             │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 상세 설계

### 4.1 DDL - MATCHING_RESULTS 테이블

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`
**변경 유형**: 수정

- `CLAIMED BOOLEAN DEFAULT FALSE` 컬럼 추가 (EXCLUDED 아래에 배치)
- 인덱스: `INDEX idx_matching_target_email_claimed (TARGET_EMAIL, CLAIMED)` 추가
  - B가 자신을 claim한 목록을 조회하는 쿼리 `targetEmail = ? AND claimed = true`를 커버

### 4.2 Domain - MatchingResult

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResult.kt`
**변경 유형**: 수정

- 생성자에 `claimed: Boolean` 파라미터 추가
- `var claimed: Boolean = claimed` (private set) 프로퍼티 추가 (excluded 패턴과 동일)
- `fun claim()` 메서드 추가 - claimed를 true로 변경
- `fun validateNotClaimed()` 메서드 추가 - 이미 claimed인 경우 예외 발생 (중복 claim 방지)
- **포인트**: exclude/include 패턴과 동일한 상태 변경 방식. 도메인 객체가 스스로 상태를 변경

### 4.3 Domain - MatchingResultRepository (Port)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/port/MatchingResultRepository.kt`
**변경 유형**: 수정

- `fun updateClaimed(matchingResult: MatchingResult)` 추가
- `fun findClaimedByTarget(email: Email): List<MatchingResult>` 추가
- **포인트**: 기존 `updateExcluded` 패턴과 동일. 포트 반환타입은 `List<MatchingResult>` (일급 컬렉션 아님)

### 4.4 Domain - MatchingResultCommandService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultCommandService.kt`
**변경 유형**: 수정

- `fun claim(matchingResultId: Long, email: String)` 메서드 추가
- 흐름: findOne → validateOwnership → validateNotClaimed → claim() → updateClaimed
- **포인트**: exclude 메서드와 동일한 패턴. 소유권 검증 후 도메인 객체에 상태 변경 위임

### 4.5 Domain - MatchingResultQueryService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryService.kt`
**변경 유형**: 수정

- `fun findClaimedBy(email: String): MatchingResultsWithProfiles` 메서드 추가
- 흐름: findClaimedByTarget → MatchingResults 생성 → extractRegisterEmails → Members/Photos 조회 → combineWithProfiles
- **포인트**: 기존 find() 메서드와 유사하나, targetEmail로 조회하고 프로필은 registerEmail 기준으로 조합. 즉 "나를 claim한 사람들"의 프로필을 보여줌
- MatchingResults에 `extractRegisterEmails()` 메서드 필요

### 4.6 Domain - MatchingResults (일급 컬렉션)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResults.kt`
**변경 유형**: 수정

- `fun extractRegisterEmails(): Set<Email>` 메서드 추가
- 기존 `extractTargetEmails()`와 대칭되는 메서드
- `combineWithClaimerProfiles(members: Members, photos: MemberPhotos): MatchingResultsWithProfiles` 메서드 추가
  - 기존 `combineWithProfiles`와 유사하나 registerEmail 기준으로 프로필 매핑

### 4.7 Infrastructure - MatchingResultTable

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/entity/table/MatchingResultTable.kt`
**변경 유형**: 수정

- `val claimed = bool("CLAIMED").clientDefault { false }` 추가

### 4.8 Infrastructure - MatchingResultEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/entity/MatchingResultEntity.kt`
**변경 유형**: 수정

- `val claimed: Boolean` 필드 추가
- `toDomain()` 메서드에 `claimed = claimed` 매핑 추가
- `from(row: ResultRow)` 에 `claimed = row[MatchingResultTable.claimed]` 추가

### 4.9 Infrastructure - MatchingResultCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultCommandDao.kt`
**변경 유형**: 수정

- `fun updateClaimed(matchingResult: MatchingResult)` 메서드 추가
- `MatchingResultTable.update({ id eq matchingResult.id }) { it[claimed] = matchingResult.claimed }` 패턴
- 기존 saveAll에 `this[MatchingResultTable.claimed] = false` 매핑 추가
- **포인트**: updateExcluded와 동일한 패턴

### 4.10 Infrastructure - MatchingResultQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/dao/MatchingResultQueryDao.kt`
**변경 유형**: 수정

- `fun findClaimedByTarget(email: String): List<MatchingResultEntity>` 메서드 추가
- 조건: `targetEmail eq email AND claimed eq true`
- `activeRows` 사용 (soft delete 필터)

### 4.11 Infrastructure - MatchingResultCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/matching/repository/MatchingResultCoreRepository.kt`
**변경 유형**: 수정

- `override fun updateClaimed(matchingResult: MatchingResult)` 추가 - commandDao.updateClaimed 위임
- `override fun findClaimedByTarget(email: Email): List<MatchingResult>` 추가 - queryDao.findClaimedByTarget → toDomain

### 4.12 Boot - MatchingResultCommandApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultCommandApi.kt`
**변경 유형**: 수정

- `@PatchMapping("/{matchingResultId}/claim") fun claim(...)` 메서드 추가
- 파라미터: `@AuthenticationPrincipal email: String`, `@PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long`
- 기존 exclude/include 엔드포인트와 동일한 패턴

### 4.13 Boot - MatchingResultQueryApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApi.kt`
**변경 유형**: 수정

- `@GetMapping("/claimed-by") fun findClaimedByMe(...)` 메서드 추가
- 파라미터: `@AuthenticationPrincipal email: String`
- 반환: `MatchingResultsResponse`
- **포인트**: 기존 MatchingResultsResponse를 재활용하되, 프로필은 registerEmail(claim한 사람) 기준

### 4.14 MatchingResultResponse 수정

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/MatchingResultResponse.kt`
**변경 유형**: 수정

- `val claimed: Boolean` 필드 추가
- `from()` 팩토리에서 `claimed = result.matchingResult.claimed` 매핑

---

## 5. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `infrastructure/.../script/ddl.sql` | 수정 | CLAIMED 컬럼 + 복합 인덱스 추가 |
| 2 | `domain/.../matching/domain/MatchingResult.kt` | 수정 | claimed 프로퍼티, claim(), validateNotClaimed() 추가 |
| 3 | `domain/.../matching/domain/MatchingResults.kt` | 수정 | extractRegisterEmails(), combineWithClaimerProfiles() 추가 |
| 4 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | updateClaimed, findClaimedByTarget 메서드 추가 |
| 5 | `infrastructure/.../entity/table/MatchingResultTable.kt` | 수정 | claimed 컬럼 정의 추가 |
| 6 | `infrastructure/.../entity/MatchingResultEntity.kt` | 수정 | claimed 필드, toDomain()/from() 매핑 |
| 7 | `infrastructure/.../dao/MatchingResultCommandDao.kt` | 수정 | updateClaimed(), saveAll 매핑 추가 |
| 8 | `infrastructure/.../dao/MatchingResultQueryDao.kt` | 수정 | findClaimedByTarget() 추가 |
| 9 | `infrastructure/.../repository/MatchingResultCoreRepository.kt` | 수정 | 포트 구현 2개 추가 |
| 10 | `domain/.../application/MatchingResultCommandService.kt` | 수정 | claim() 메서드 추가 |
| 11 | `domain/.../application/MatchingResultQueryService.kt` | 수정 | findClaimedBy() 메서드 추가 |
| 12 | `boot/.../api/response/MatchingResultResponse.kt` | 수정 | claimed 필드 추가 |
| 13 | `boot/.../api/MatchingResultCommandApi.kt` | 수정 | PATCH /{id}/claim 엔드포인트 추가 |
| 14 | `boot/.../api/MatchingResultQueryApi.kt` | 수정 | GET /claimed-by 엔드포인트 추가 |

---

## 6. 고려사항

- **중복 claim 방지**: 이미 claimed인 상태에서 다시 claim하면 예외를 발생시킨다. `validateNotClaimed()`에서 처리
- **unclaim(취소) 기능**: 현재 범위에서 제외. 필요 시 exclude/include 패턴처럼 `unclaim()` 메서드와 `PATCH /{id}/unclaim` 엔드포인트를 추가하면 됨
- **인덱스**: `(TARGET_EMAIL, CLAIMED)` 복합 인덱스로 B의 "나를 claim한 목록" 조회 성능을 확보. 기존 `idx_matching_register_email`은 A 기준 조회에 사용
- **FK 안전성**: 기존 테이블에 컬럼만 추가하므로 FK 관련 이슈 없음
- **알림 확장 포인트**: MatchingResultCommandService.claim() 내에서 claim 완료 후 이벤트를 발행하는 방식으로 확장 가능. 현재는 구현하지 않되, Service 메서드가 단일 책임이므로 나중에 ApplicationEventPublisher를 주입하여 ClaimEvent를 발행하면 됨
- **claimed-by 조회 시 프로필 매핑 방향**: 기존 find()는 targetEmail 기준 프로필을 보여주지만, findClaimedBy()는 registerEmail(claim한 사람) 기준 프로필을 보여줘야 함. MatchingResults에 별도 combineWithClaimerProfiles() 메서드를 추가하여 처리
- **X룸 입장 연동**: claimed=true 여부는 향후 X룸 입장 권한 체크 시 MatchingResultRepository를 통해 조회하면 됨. 별도 테이블/도메인 없이 기존 구조로 충분

---

## 7. 테스트

| 대상 | 테스트 내용 |
|------|-------------|
| MatchingResult | claim() 호출 시 claimed=true, validateNotClaimed() 중복 방지 |
| MatchingResultCommandService | claim 흐름 (소유권 검증, 중복 방지, 상태 변경) |
| MatchingResultQueryService | findClaimedBy 흐름 (프로필 조합 포함) |
| MatchingResultCommandDao | updateClaimed DB 반영 확인 |
| MatchingResultQueryDao | findClaimedByTarget 조건 검증 |
| MatchingResultCommandApi | PATCH /claim REST Docs |
| MatchingResultQueryApi | GET /claimed-by REST Docs |

---

## 8. api-todo.md 업데이트

완료 후 "상대방에게 당신이 나의 X임을 표시하기" 항목을 완료 섹션으로 이동:

| Method | Endpoint | 용도 |
|--------|----------|------|
| PATCH | /api/matching-results/{matchingResultId}/claim | 매칭 결과 claim 표시 |
| GET | /api/matching-results/claimed-by | 나를 claim한 목록 조회 |
