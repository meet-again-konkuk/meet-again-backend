# Plan: 매칭 결과 claim 거절하기 (`PATCH /api/matching-results/{id}/reject`)

- 작성일: 2026-07-17
- 작업 유형: 기능 개발 (상태 모델 마이그레이션 포함)
- 브랜치: `feat/matching-claim-reject`
- 상태: Confirmed (스펙 사용자 확정 — 거절만 구현, id 기반)
- **대체 관계**: 본 문서는 `docs/plan/202604/claim-accept-reject.plan.md` 초안을 **대체**한다.
  차이 두 가지: ① **거절만** 구현(accept 미도입, `ClaimStatus`는 3값 `NONE/CLAIMED/REJECTED`), ② 초안은 Email 기반이었으나 이후 member-id-ref 마이그레이션으로 현행 코드는 **id 기반**(`registerId`/`targetId: Long`, `memberInfo.id`)이라 전면 재설계. 202604 초안은 수정하지 않는다(이력 보존).

---

## 1. 요구사항 요약

- 수신자(target, claim 당한 당사자 B)가 자신에게 온 claim을 **거절**한다. `PATCH /api/matching-results/{matchingResultId}/reject`.
- 인증 필요. 요청 본문 없음(path + auth 로 상태 결정). 성공 응답은 기존 claim/exclude/include 컨벤션과 동일(200 OK, 본문 없음).
- 상태 모델을 `claimed: Boolean` → `ClaimStatus` enum(`NONE`, `CLAIMED`, `REJECTED`)으로 전환한다. `ACCEPTED`는 이번에 도입하지 않되, 추후 `CLAIMED → ACCEPTED` 전이만 추가하면 되도록 확장 가능한 구조로 만든다.
- 새 도메인을 만들지 않는다 — 기존 `matching` 도메인 `MatchingResult` 애그리거트의 상태 전이로 처리한다.

### 용어

- **A = 등록자(register)** = `registerId`. targetInfo를 등록하고 "이 사람이 내 X"라고 claim 하는 쪽.
- **B = 수신자(target)** = `targetId`. claim을 당하는 당사자. **거절은 B만 할 수 있다.**

---

## 2. 상태 모델 (`ClaimStatus`) 전이 규칙

| 현재 상태 | 이벤트(호출자) | 다음 상태 | 위반 시 |
|-----------|----------------|-----------|---------|
| `NONE` (기본값) | `claim()` — A | `CLAIMED` | — |
| `CLAIMED` | `reject()` — B | `REJECTED` (최종) | — |
| `NONE` | `reject()` | ❌ 불가 | `InvalidStateException` ("claim 상태가 아님") |
| `CLAIMED` | `claim()` | ❌ 불가 | `InvalidStateException` ("이미 claim됨") — 기존 동작 유지 |
| `REJECTED` | `claim()` / `reject()` | ❌ 불가 (최종) | `InvalidStateException` |

- `REJECTED`는 **최종 상태**. 재claim/재거절 없음(본 범위). A의 claim 취소(unclaim), 거절 취소는 범위 밖(§9).
- **확장점**: 추후 `accept()` 추가 시 `CLAIMED → ACCEPTED` 전이 1개와 enum 값 1개만 추가하면 된다. 전이 검증은 `MatchingResult` 내부에 캡슐화되어 있어 다른 레이어 변경 없이 확장 가능.

---

## 3. 확정 결정 사항

| # | 쟁점 | 조사 결과 / 근거 | 결정 |
|---|------|-----------------|------|
| **D1** | claim 상태 표현 | 현행 `claimed: Boolean`은 이미 "claim했다"만 표현. 거절 상태를 담으려면 boolean 2개(불가능 상태 발생) 또는 enum. | **`ClaimStatus` enum 단일 컬럼(`NONE/CLAIMED/REJECTED`)**. 한 claim의 라이프사이클을 한 row/컬럼으로 표현. `ACCEPTED`는 미도입(값 미추가). |
| **D2** | 거절 소유권 검증 | 현행 `validateOwnership(memberId)`는 `registerId` 기준(=A). 거절은 B 기준 검증 필요. | **`validateTargetOwnership(memberId)` 신설** — `targetId != memberId`면 `AccessDeniedException`. 202604 초안 §4.2의 id 버전. |
| **D3** | 전이 위반 예외/상태코드 | `GlobalExceptionHandler` 실측: `InvalidStateException` → **400**, `AccessDeniedException` → 403, `EntityNotFoundException` → 404, `DuplicateException` → 409. | **`InvalidStateException` → 400 사용**. ⚠️ 스펙의 "409류"와 다름 — 이 코드베이스에서 상태 전이 위반은 400이며, **기존 claim 중복(`이미 claim됨`)도 동일하게 400**이라 일관성 위해 400 채택. 별도 409 예외 신설 안 함(§7 리스크 참조). |
| **D4** | `findClaimedByTarget` 필터 의미 변경 | **핵심 발견**: 이 포트/DAO는 현재 **4곳**이 공유(§6). 조건을 `claimStatus == CLAIMED`로 바꾸면 REJECTED가 claimer 목록뿐 아니라 **X룸 received/access/탈퇴백업에서도 사라진다.** | **단일 쿼리 유지 + `claimStatus eq CLAIMED` 필터** 채택. "거절 = 연결 해제" 모델로 일관(§6.2). REJECTED는 최종이라 재노출 우려 없음. **대안(쿼리 분리)은 §6.3에 명시** — 프론트/기획이 "거절해도 X룸은 유지"를 원하면 그때 전환. |
| **D5** | `MatchingResultResponse.claimed` (A의 `/api/matching-results` 목록 필드) | 현재 `claimed: Boolean` 노출. enum 전환 후에도 프론트 계약 유지 필요. | **필드명 `claimed: Boolean` 유지**, 매핑을 `result.matchingResult.isClaimed()`(= `claimStatus != NONE`)로 교체. **하위호환**(응답 스키마·Vocabulary 무변경). enum 노출은 후속 옵션. |
| **D6** | DB 저장 형식 | enum 순서 저장은 값 추가 시 깨짐(code-implementation-rules). | **`enumerationByName("CLAIM_STATUS", 20, ClaimStatus::class)`** — VARCHAR 문자열 저장. 컬럼 `CLAIMED BOOLEAN` → `CLAIM_STATUS VARCHAR(20) NOT NULL DEFAULT 'NONE'`. |
| **D7** | 인덱스 | 현행 DDL 실측: `MATCHING_RESULTS`엔 `idx_matching_target_id (TARGET_ID)`만 있고 claimed 복합 인덱스 없음(202604 초안이 가정한 `(TARGET_EMAIL, CLAIMED)` 인덱스는 현행에 부재). | **`idx_matching_target_id (TARGET_ID)` → `idx_matching_target_id_claim_status (TARGET_ID, CLAIM_STATUS)` 복합으로 교체**(선택적 최적화, left-prefix로 기존 TARGET_ID 단독 조회도 커버). `findClaimedByTarget`의 4개 소비처 쿼리를 커버. |

---

## 4. 아키텍처

```
┌───────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                        │
│  MatchingResultCommandApi                                              │
│    └── PATCH /{id}/reject → commandService.reject(id, memberInfo.id)   │  ← 신규 엔드포인트
│  MatchingResultResponse.claimed = result.matchingResult.isClaimed()   │  ← 매핑 교체(하위호환)
└──────────────────────────────┬────────────────────────────────────────┘
                               │ (port)
┌──────────────────────────────▼────────────────────────────────────────┐
│ domain/ma-domain-core (matching)                                       │
│  MatchingResultCommandService.reject(matchingResultId, memberId)      │  ← 신규
│    1) findOne(id) → MatchingResult   (없으면 EntityNotFound → 404)     │
│    2) validateTargetOwnership(memberId)  (B 아니면 AccessDenied → 403) │
│    3) reject()   (CLAIMED 아니면 InvalidState → 400)                   │
│    4) updateClaimStatus(matchingResult)                                │
│                                                                        │
│  MatchingResult (도메인)                                               │
│    - claimStatus: ClaimStatus  (private set)   ← claimed:Boolean 대체  │
│    + validateTargetOwnership(memberId)         ← 신규 (targetId 기준)  │
│    + claim()   NONE → CLAIMED  (재작성)                                │
│    + reject()  CLAIMED → REJECTED  (신규)                              │
│    + isClaimed(): claimStatus != NONE  (신규, Response용 파생)         │
│  ClaimStatus (enum): NONE / CLAIMED / REJECTED   ← 신규                 │
│                                                                        │
│  MatchingResultRepository (port)                                       │
│    + updateClaimStatus(matchingResult)   ← updateClaimed 대체          │
│    · findClaimedByTarget(memberId)  (내부 조건만 변경, 시그니처 동일)  │
└──────────────────────────────┬────────────────────────────────────────┘
                               │ (implements)          ▲ findClaimedByTarget 공유 소비처(§6):
┌──────────────────────────────▼──────────────┐        │  · MatchingResultQueryService.findClaimedBy (/api/claimers/me)
│ infrastructure/storage/ma-db-core            │        │  · XroomQueryService.findReceived (/api/xrooms/received)
│  MatchingResultTable.claimStatus =           │        │  · XroomValidator.validateAccessible (xroom 상세 접근)
│    enumerationByName("CLAIM_STATUS",20,…)    │        │  · MemberWithdrawalBackupCollector (탈퇴 백업)
│  MatchingResultEntity.claimStatus            │        └─────────────────────────────────────────────
│  MatchingResultCommandDao.updateClaimStatus  │
│  MatchingResultQueryDao.findClaimedByTarget  │   ← 조건: claimStatus eq CLAIMED
│  MatchingResultCoreRepository                │
│  ddl.sql: CLAIM_STATUS VARCHAR(20)           │
└──────────────────────────────────────────────┘
```

---

## 5. `claimed` Boolean 참조 전수 조사 & 마이그레이션 방법

`grep` 실측 결과. **프로덕션 10 + 테스트/픽스처 9 = 19개 파일** 영향.

### 5.1 프로덕션 코드

| # | 파일 | 현행 | 마이그레이션 |
|---|------|------|-------------|
| P1 | `domain/.../matching/domain/MatchingResult.kt` | `claimed: Boolean` 생성자 param + `var claimed`(private set) + `claim()` + `validateNotClaimed()` | `claimStatus: ClaimStatus` 로 교체. `claim()` 재작성(NONE→CLAIMED), `reject()`·`validateTargetOwnership()`·`isClaimed()` 추가. `validateNotClaimed()` 제거(전이 검증에 흡수) |
| P2 | `domain/.../matching/domain/ClaimStatus.kt` | (없음) | **신규** enum `NONE/CLAIMED/REJECTED` |
| P3 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | `updateClaimed(...)`, `findClaimedByTarget(memberId)` | `updateClaimed` → `updateClaimStatus`. `findClaimedByTarget` 시그니처 유지 |
| P4 | `domain/.../matching/application/MatchingResultCommandService.kt` | `claim()` 내부 `updateClaimed` 호출 | `updateClaimStatus`로 교체 + `reject(matchingResultId, memberId)` 추가 |
| P5 | `infrastructure/.../entity/table/MatchingResultTable.kt` | `val claimed = bool("CLAIMED").clientDefault{false}` | `val claimStatus = enumerationByName("CLAIM_STATUS",20,ClaimStatus::class).clientDefault{ClaimStatus.NONE}` |
| P6 | `infrastructure/.../entity/MatchingResultEntity.kt` | `claimed: Boolean` + `toDomain()`/`from()` 매핑 | `claimStatus: ClaimStatus`로 교체 + 매핑 |
| P7 | `infrastructure/.../dao/MatchingResultCommandDao.kt` | `updateClaimed(...)`, `saveAll`의 `this[...claimed]=false` | `updateClaimStatus(...)`, `saveAll`의 `this[...claimStatus]=ClaimStatus.NONE` |
| P8 | `infrastructure/.../dao/MatchingResultQueryDao.kt` | `findClaimedByTarget`: `targetId eq … and claimed eq true` | `… and claimStatus eq ClaimStatus.CLAIMED` |
| P9 | `infrastructure/.../repository/MatchingResultCoreRepository.kt` | `override fun updateClaimed` | `override fun updateClaimStatus` 위임 |
| P10 | `infrastructure/.../resources/script/ddl.sql` | `CLAIMED BOOLEAN DEFAULT FALSE` + `idx_matching_target_id` | `CLAIM_STATUS VARCHAR(20) NOT NULL DEFAULT 'NONE'` + 복합 인덱스 교체(§8) |
| P11 | `boot/.../matching/api/response/MatchingResultResponse.kt` | `claimed = result.matchingResult.claimed` | `claimed = result.matchingResult.isClaimed()` (필드명·타입 유지, D5) |
| P12 | `boot/.../matching/api/MatchingResultCommandApi.kt` | claim/exclude/include | `reject` 엔드포인트 추가 |

### 5.2 테스트 / 픽스처

| # | 파일 | 마이그레이션 |
|---|------|-------------|
| T1 | `domain/.../testFixtures/.../matching/fixture/MatchingResultFixture.kt` | `claimed: Boolean = false` param → `claimStatus: ClaimStatus = ClaimStatus.NONE`, 매핑 교체 |
| T2 | `domain/.../test/.../matching/domain/MatchingResultTest.kt` | `claim()` 전이 테스트 유지·보강 + `reject()`/`validateTargetOwnership()`/`isClaimed()` 테스트 추가 |
| T3 | `domain/.../test/.../matching/application/MatchingResultCommandServiceTest.kt` | `reject` 흐름 테스트 추가(정상/소유권 위반/상태 위반) |
| T4 | `infrastructure/.../test/.../matching/dao/MatchingResultQueryDaoTest.kt` | `insertMatchingResult(claimed=…)` 헬퍼 → `claimStatus`, `findClaimedByTarget`가 CLAIMED만·REJECTED 제외 검증 |
| T5 | `infrastructure/.../test/.../matching/dao/MatchingResultCommandDaoTest.kt` | `updateClaimStatus` 저장(VARCHAR) 검증으로 교체 |
| T6 | `boot/.../test/.../matching/api/MatchingResultCommandApiTest.kt` | `reject` REST Docs + 403/404/400 실패 케이스 |
| T7 | `boot/.../test/.../integration/XroomIntegrationTest.kt` | 헬퍼 `claimed: Boolean=true` param, `it[MatchingResultTable.claimed]=…` (3+곳) → `claimStatus`. **REJECTED가 received/access에서 제외됨을 검증하는 회귀 케이스 추가(§6)** |
| T8 | `domain/.../test/.../xroom/domain/XroomValidatorTest.kt` | `claimed = true` 픽스처 → `claimStatus = ClaimStatus.CLAIMED` |
| T9 | `infrastructure/support/ma-file-storage/.../JacksonMemberBackupSerializerTest.kt` | `MatchingResultFixture.create(...)` — 픽스처 param만 바뀌므로 컴파일 정합만 확인 |

> **유지(변경 불필요)**: `boot/.../vocabulary/MatchingVocabulary.kt`의 `claimed()`(`matchingResults[].claimed`)와 `MatchingResultQueryApiTest.kt`의 사용부 — 응답 필드 `claimed: Boolean`을 D5로 그대로 유지하므로 그대로 둔다.

---

## 6. `findClaimedByTarget` 의미 변경의 블라스트 반경 (핵심 발견)

> 202604 초안(Email 시절)은 `findClaimedByTarget`를 `/api/claimers`만의 쿼리로 보았다. **현행 id 기반 코드에서는 4곳이 공유**한다. `claimed eq true` → `claimStatus eq CLAIMED`(REJECTED 제외) 변경이 이 4곳 전부에 전파된다.

### 6.1 소비처 4곳과 영향

| 소비처 | 경로 | REJECTED 제외의 효과 | 판정 |
|--------|------|----------------------|------|
| `MatchingResultQueryService.findClaimedBy` | `GET /api/claimers/me` | B가 거절한 claim이 claimer 목록에서 사라짐 | ✅ **의도됨** (스펙: "거절한 claim은 다시 안 보임") |
| `XroomQueryService.findReceived` | `GET /api/xrooms/received` | B가 거절하면 A가 만든 X룸이 "받은 X룸" 목록에서 사라짐 | ⚠️ **의도적 부수효과** — "거절=연결 해제"로 일관(6.2) |
| `XroomValidator.validateAccessible` | X룸 상세 접근 | B가 거절 후엔 A의 X룸 상세 접근 불가(403) | ⚠️ 위와 동일 |
| `MemberWithdrawalBackupCollector` | 탈퇴 백업 | REJECTED claim은 탈퇴 백업에 미포함 | ⚠️ 경미 — 복원 시 거절 이력 유실(6.2) |

### 6.2 채택: 단일 쿼리 + "거절 = 연결 해제" 모델 (D4)

`findClaimedByTarget`를 `claimStatus == CLAIMED` 단일 조건으로 두고 4곳 모두에 동일 적용한다. 근거:
- 스펙의 UX 전제("거절한 claim이 다시 안 보임")를 X룸까지 자연스럽게 확장 — B가 A를 거절하면 A가 B를 대상으로 만든 X룸도 더는 B에게 보이지 않는 것이 일관적이다.
- `REJECTED`는 최종 상태라 재노출/재활성 우려 없음.
- 변경 지점이 쿼리 조건 1곳으로 최소.
- 기존 X룸 테스트는 모두 `claimed = true`(→ `CLAIMED`)라 회귀 영향 없음. 새 행동(REJECTED 제외)은 신규 케이스로만 나타남.

### 6.3 대안(미채택): 소비처별 쿼리 분리

프론트/기획이 "거절해도 이미 공유된 X룸은 유지"를 원하면 아래로 전환:
- `findClaimedByTarget(memberId)` → `findByTargetAndStatus(memberId, status)` 또는 `findClaimedByTarget`(claimer 목록: CLAIMED만) + `findConnectedByTarget`(X룸: `claimStatus != NONE`, 즉 REJECTED 포함) 2개로 분리.
- 비용: 포트/DAO/CoreRepository에 메서드 1개 추가, X룸 3소비처를 새 메서드로 교체.
- **판단 필요 지점** — 구현 착수 전 프론트와 1줄 확인 권장. 확인 전까지는 6.2(단일 쿼리)로 진행하되, 회귀 테스트(T7)에서 이 행동을 명시적으로 고정한다.

---

## 7. 에러 케이스 (실측 기반)

| HTTP | 상황 | 예외 | 발생 위치 |
|------|------|------|-----------|
| **403** Forbidden | 호출자가 수신자(B, `targetId`)가 아님 | `AccessDeniedException` | `MatchingResult.validateTargetOwnership` |
| **404** Not Found | `matchingResultId` 부재 / soft-deleted | `EntityNotFoundException` | `MatchingResultCoreRepository.findOne` |
| **400** Bad Request | `claimStatus`가 `CLAIMED`가 아님 (NONE=아직 claim 안됨 / REJECTED=이미 거절됨) | `InvalidStateException` | `MatchingResult.reject` |

> ⚠️ **스펙의 "409류"와 실제가 다름**: `GlobalExceptionHandler`에서 `InvalidStateException`은 **400**으로 매핑된다(409는 `DuplicateException` 전용). 기존 claim 중복 처리도 `InvalidStateException`→400이므로, 거절도 400으로 두는 것이 일관적이다. 굳이 409를 원하면 새 예외+핸들러가 필요하나 기존 컨벤션과 어긋나므로 **비권장**.

---

## 8. DDL 변경

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql` (`MATCHING_RESULTS`)

- 컬럼 교체:
  - `CLAIMED BOOLEAN DEFAULT FALSE` **제거**
  - `CLAIM_STATUS VARCHAR(20) NOT NULL DEFAULT 'NONE'` **추가** (EXCLUDED 아래 위치)
- 인덱스 교체(D7):
  - `INDEX idx_matching_target_id (TARGET_ID)` **제거**
  - `INDEX idx_matching_target_id_claim_status (TARGET_ID, CLAIM_STATUS)` **추가**
- **기존 데이터 마이그레이션 규칙**(운영 반영 시):
  ```sql
  ALTER TABLE MATCHING_RESULTS ADD COLUMN CLAIM_STATUS VARCHAR(20) NOT NULL DEFAULT 'NONE';
  UPDATE MATCHING_RESULTS SET CLAIM_STATUS = 'CLAIMED' WHERE CLAIMED = TRUE;
  ALTER TABLE MATCHING_RESULTS DROP COLUMN CLAIMED;
  -- 인덱스 교체
  ALTER TABLE MATCHING_RESULTS DROP INDEX idx_matching_target_id;
  ALTER TABLE MATCHING_RESULTS ADD INDEX idx_matching_target_id_claim_status (TARGET_ID, CLAIM_STATUS);
  ```
  개발 환경은 DDL 재적용 전제(스크립트 반영). FK 미사용이라 컬럼 교체 안전.

---

## 9. 파일별 상세 설계 (컴파일 수준 스니펫)

### N1. `ClaimStatus.kt` (신규)
`domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/ClaimStatus.kt`
```kotlin
package com.konkuk.ma.domain.matching.domain

enum class ClaimStatus {
    NONE,     // claim 전 (기본값)
    CLAIMED,  // 등록자(A)가 claim 함 — 수신자(B) 응답 대기
    REJECTED, // 수신자(B)가 거절함 (최종)
    // 확장점: 추후 ACCEPTED 추가 시 값 1개 + MatchingResult.accept() 만 추가
}
```

### M1. `MatchingResult.kt` (수정)
```kotlin
class MatchingResult(
    val id: Long,
    val registerId: Long,
    override val targetInfoId: Long,
    override val targetId: Long,
    // ... matched 플래그들 동일 ...
    val showingExpiryDate: LocalDateTime,
    val matchingExpiryDate: LocalDate,
    excluded: Boolean,
    claimStatus: ClaimStatus = ClaimStatus.NONE,   // ← claimed: Boolean 대체
) : HasMatchingKey {
    var excluded: Boolean = excluded
        private set
    var claimStatus: ClaimStatus = claimStatus
        private set

    // ... matchRate / isVisible / getRemainingDays / exclude / include 동일 ...

    fun validateOwnership(memberId: Long) {          // 기존 유지 (A 기준)
        if (registerId != memberId) {
            throw AccessDeniedException(EntityType.MATCHING_RESULT, registerId.toString(), memberId.toString())
        }
    }

    fun validateTargetOwnership(memberId: Long) {    // ← 신규 (B 기준)
        if (targetId != memberId) {
            throw AccessDeniedException(EntityType.MATCHING_RESULT, targetId.toString(), memberId.toString())
        }
    }

    fun claim() {                                    // ← 재작성 (NONE → CLAIMED)
        if (claimStatus != ClaimStatus.NONE) {
            throw InvalidStateException(MatchingResult::class, id, "이미 claim 처리된 매칭 결과입니다.")
        }
        claimStatus = ClaimStatus.CLAIMED
    }

    fun reject() {                                   // ← 신규 (CLAIMED → REJECTED)
        if (claimStatus != ClaimStatus.CLAIMED) {
            throw InvalidStateException(MatchingResult::class, id, "claim 상태가 아니어서 거절할 수 없습니다.")
        }
        claimStatus = ClaimStatus.REJECTED
    }

    fun isClaimed(): Boolean = claimStatus != ClaimStatus.NONE   // ← Response.claimed 파생용
}
```

### M2. `MatchingResultRepository.kt` (port, 수정)
```kotlin
// updateClaimed(...) 제거 → 추가:
fun updateClaimStatus(matchingResult: MatchingResult)
// findClaimedByTarget(memberId: Long): List<MatchingResult>  — 시그니처 유지
```

### M3. `MatchingResultCommandService.kt` (수정)
```kotlin
fun claim(matchingResultId: Long, memberId: Long) {
    val matchingResult = matchingResultRepository.findOne(matchingResultId)
    matchingResult.validateOwnership(memberId)          // A 기준
    matchingResult.claim()
    matchingResultRepository.updateClaimStatus(matchingResult)   // ← updateClaimed 교체
}

fun reject(matchingResultId: Long, memberId: Long) {    // ← 신규
    val matchingResult = matchingResultRepository.findOne(matchingResultId)
    matchingResult.validateTargetOwnership(memberId)    // B 기준
    matchingResult.reject()
    matchingResultRepository.updateClaimStatus(matchingResult)
}
```

### M4. `MatchingResultTable.kt` (수정)
```kotlin
// val claimed = bool("CLAIMED").clientDefault { false }  ← 제거
val claimStatus = enumerationByName("CLAIM_STATUS", 20, ClaimStatus::class)
    .clientDefault { ClaimStatus.NONE }
```

### M5. `MatchingResultEntity.kt` (수정)
- 필드 `val claimed: Boolean` → `val claimStatus: ClaimStatus`
- `toDomain()`의 `claimed = claimed` → `claimStatus = claimStatus`
- `from(row)`의 `claimed = row[...claimed]` → `claimStatus = row[MatchingResultTable.claimStatus]`

### M6. `MatchingResultCommandDao.kt` (수정)
```kotlin
fun updateClaimStatus(matchingResult: MatchingResult) {          // ← updateClaimed 교체
    MatchingResultTable.update({ MatchingResultTable.id eq matchingResult.id }) {
        it[claimStatus] = matchingResult.claimStatus
    }
}
// saveAll: this[MatchingResultTable.claimed] = false → this[MatchingResultTable.claimStatus] = ClaimStatus.NONE
```

### M7. `MatchingResultQueryDao.kt` (수정)
```kotlin
fun findClaimedByTarget(memberId: Long): List<MatchingResultEntity> {
    return MatchingResultTable
        .activeRows { (MatchingResultTable.targetId eq memberId) and
                      (MatchingResultTable.claimStatus eq ClaimStatus.CLAIMED) }  // ← claimed eq true 교체
        .map { row -> MatchingResultEntity.from(row) }
}
```

### M8. `MatchingResultCoreRepository.kt` (수정)
```kotlin
override fun updateClaimStatus(matchingResult: MatchingResult) {   // ← updateClaimed 교체
    matchingResultCommandDao.updateClaimStatus(matchingResult)
}
```

### M9. `MatchingResultCommandApi.kt` (수정)
```kotlin
@PatchMapping("/{matchingResultId}/reject")
fun reject(
    @LoginMember memberInfo: MemberInfo,
    @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
) {
    matchingResultCommandService.reject(matchingResultId, memberInfo.id)
}
```

### M10. `MatchingResultResponse.kt` (수정)
```kotlin
// from(): claimed = result.matchingResult.isClaimed()   ← .claimed 대체 (D5, 하위호환)
```

### M11. `MatchingResultFixture.kt` (testFixtures, 수정)
```kotlin
// param: claimStatus: ClaimStatus = ClaimStatus.NONE  (claimed: Boolean 대체)
// 매핑: claimStatus = claimStatus
```

---

## 10. 구현 순서 (TDD: RED → GREEN, 의존성 순서)

각 단계는 **테스트 선작성 → RED 확인 → 구현 → GREEN 확인** 사이클. 아래 순서는 컴파일 의존성 순.

| Step | 대상 | 내용 | RED 기준 |
|------|------|------|----------|
| **1** | 도메인 모델 | (테스트 T2) `MatchingResultTest`에 `reject()` 정상 전이·비CLAIMED 예외·`validateTargetOwnership` 성공/실패·`isClaimed()` 케이스 작성 → RED → `ClaimStatus`(N1)·`MatchingResult`(M1) 구현 → GREEN | `reject`/`ClaimStatus` 미존재로 컴파일 실패 |
| **2** | 픽스처 | `MatchingResultFixture`(T1) `claimStatus` 파라미터화 (Step1 테스트가 이걸 사용) | — (컴파일 정합) |
| **3** | 포트 | `MatchingResultRepository`(M2) `updateClaimStatus` 교체 | — |
| **4** | 애플리케이션 서비스 | (테스트 T3) `MatchingResultCommandServiceTest`에 `reject` 정상/소유권 위반(403 유발 예외)/상태 위반 mock 시나리오 → RED → `MatchingResultCommandService.reject`(M3) 구현 → GREEN | `reject` 미존재 |
| **5** | 인프라 (table/entity/dao/repo) | M4~M8 교체. (테스트 T4/T5) `MatchingResultQueryDaoTest`: `findClaimedByTarget`가 CLAIMED만 반환·REJECTED 제외 / `MatchingResultCommandDaoTest`: `updateClaimStatus` VARCHAR 저장 검증 → RED → 구현 → GREEN | `claimStatus` 컬럼/메서드 미존재 |
| **6** | DDL | `ddl.sql`(P10, §8) 컬럼·인덱스 교체 (Step5 DAO 통합테스트가 실제 스키마 사용) | 통합테스트 스키마 불일치 |
| **7** | 웹 계층 | M9(`reject` 엔드포인트)·M10(`Response.isClaimed()`). (테스트 T6) `MatchingResultCommandApiTest`: `reject` 성공(200)·403·404·400 → RED → 구현 → GREEN | 엔드포인트 404 |
| **8** | REST Docs | §11 — `reject` 스니펫 + main.adoc 링크 | — |
| **9** | 회귀 (X룸/백업) | (테스트 T7/T8) `XroomIntegrationTest`·`XroomValidatorTest`의 `claimed` → `claimStatus` 정합 + **REJECTED가 received/access에서 제외됨을 고정하는 신규 케이스**(§6.2) → GREEN | 컴파일/행동 회귀 |
| **10** | 문서/정리 | `api-todo.md`(§12), `T9` 컴파일 정합, `./gradlew build` 전체 GREEN | — |

---

## 11. REST Docs (rest-docs-generator가 수행 — 계획에만 명시)

- 기존 claim 스니펫 미러링: `matching/claim-matching-result.adoc` → **`matching/reject-matching-result.adoc`** 신규.
- `MatchingResultCommandApiTest`에 문서화 테스트 추가:
  - 정상: `PATCH /api/matching-results/{encodedId}/reject` → 200, `andDocument("matching/reject-matching-result")`
  - 실패: 소유권 없음 403 / 부재 404 / 비CLAIMED 400 (기존 claim/exclude 실패 케이스 패턴 재사용)
- `boot/.../docs/asciidoc/main.adoc` — claim 링크(`matching/claim-matching-result.html`) 인접에 reject 링크 추가.
- Path parameter만 있고 요청/응답 본문 없음 → `pathParameters` + 성공 200(본문 없음).

---

## 12. api-todo.md 갱신

**파일**: `docs/api-todo.md`

- `## 매칭 > ### X 거절하기` 항목(현재 "받아주기(claim)는 구현 완료 — 거절만 남음" 참고 문구) **제거**.
- `## 매칭` 상단 "완료된 API > 매칭" 표에 1행 추가:

  | PATCH | /api/matching-results/{id}/reject | 나를 claim한 상대 거절하기 |

- `feedback_api_todo_keep_domain` 준수: 매칭 도메인 섹션은 유지, 하위에 남은 TODO 없으면 "작업할 내용 없음" 표기.

---

## 13. 리스크 / 주의

- **[핵심] `findClaimedByTarget` 4곳 공유(§6)**: 조건 변경이 X룸 received/access/탈퇴백업에 전파된다. 6.2(단일 쿼리, 거절=연결해제) 채택하되, **프론트와 "거절 시 X룸도 사라짐" 1줄 합의** 후 착수 권장. 미합의 시에도 회귀 테스트(T7)로 행동을 고정해 사고 방지.
- **상태코드 400 vs 409(§7, D3)**: 스펙 문구("409류")와 실제 매핑(400)이 다르다. 기존 claim 중복도 400이라 일관성 위해 400 채택. 리뷰 시 재확인.
- **`Response.claimed` 하위호환(D5)**: `isClaimed()`(=`!= NONE`)로 유지해 프론트 스키마 무변경. 단 REJECTED도 `claimed=true`로 보임(A 관점 "내가 claim했다"는 사실은 참). A에게 거절 결과를 노출하려면 후속으로 enum 필드 추가.
- **동시성**: 동일 claim에 대한 claim/reject 동시 호출은 DB 최종 UPDATE 기준. 희소 케이스라 락 미도입. 엄격 보장 필요 시 후속 `SELECT ... FOR UPDATE`.
- **FK 안전성**: 컬럼 교체만, 타 테이블이 `CLAIMED` 미참조 → FK 이슈 없음(`feedback_no_fk` 준수).
- **enum 저장 형식(D6)**: `enumerationByName`(문자열). 순서 저장 금지 — 추후 ACCEPTED 삽입 위치 무관하게 안전.
- **테스트 인증**: API 테스트는 `BaseApiTest`의 `@WithAuthMember` 기본값(memberId=1) 사용, 중복 선언 금지(`feedback_no_duplicate_with_auth_member`). 403 케이스는 `targetId`를 인증 memberId와 다르게 설정.

---

## 14. 범위 밖 (후속으로만 기록)

- **받아주기(accept)**: `ClaimStatus.ACCEPTED` + `MatchingResult.accept()`(CLAIMED→ACCEPTED) + `PATCH /accept`. 본 구조에서 값·전이·엔드포인트 추가만으로 확장 가능.
- A의 **claim 취소(unclaim)**, B의 **거절 취소**.
- 거절/응답 **히스토리 조회 API**.
- 수락 시 **X룸 자동 공유 / 알림 이벤트**(`ApplicationEventPublisher` 확장점: `reject()` 성공 직후 Service에서 발행 가능).
- §6.3 **쿼리 분리**(거절해도 X룸 유지) 전환.
- (리뷰 후속) `MatchingResultCommandDao.updateClaimStatus/updateExcluded`가 도메인 객체를 직접 받는 기존 패턴 — clean-architecture의 "DAO write는 Entity 입력" 규칙과 어긋나므로 두 메서드를 쌍으로 리팩터 (이번 PR은 선례 일관성 유지).
- (리뷰 후속) `MatchingResultResponse`에 `claimStatus` enum 노출 — A가 "거절당함"을 구분하려면 프론트 협의 후 필드 추가 (현재는 하위호환 위해 `claimed: Boolean`만, Vocabulary 설명으로 의미 보정).

---

## 15. 구현 시 참조 (규칙 본문은 복제하지 않음)

- 객체/서비스 구현 규칙: [[code-implementation-rules]] (Service 조합만·분기 금지, 도메인 행위 부여, 포트 규칙, 상태 검증 도메인 내부)
- 모듈·패키지 배치: [[clean-architecture]] (matching 도메인 내부 처리, 새 도메인 미생성)
- 도메인 모델링(상태 전이/애그리거트): [[domain-driven-design]]
- 가독성/네이밍: [[clean-code]]
- 테스트 작성: [[kotest-writing]] (KoTest + Mockk, BaseApiTest)
- API 문서화: [[rest-docs-writing]] (Vocabulary 재사용, main.adoc 연결)
