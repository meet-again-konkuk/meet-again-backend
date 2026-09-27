# Plan: X 받아주기 / 거절하기 (Claim Accept / Reject)

> 작성일: 2026-04-20

## 1. 개요

매칭 결과에서 상대방(A)의 claim(= "이 사람이 내 X다")을 claim 당한 당사자(B)가 **받아주기(accept)** 또는 **거절하기(reject)** 하는 기능을 구현한다.

현재는 A의 `claim()` 까지만 구현되어 있으며, B는 `/api/claimers/me` 로 자신을 claim한 상대 목록을 조회할 수 있다. 이번 계획은 B가 그 리스트에서 각 claim에 대해 응답할 수 있도록 두 개의 command API를 추가한다.

### 상태 모델 전환 (핵심 결정)

- 기존: `MatchingResultTable.claimed: Boolean` — 단일 true/false
- 변경: `MatchingResultTable.claimStatus: Enum`(VARCHAR) — `NONE`, `CLAIMED`, `ACCEPTED`, `REJECTED`
  - 상태 전이: `NONE → CLAIMED` (A가 claim) → `ACCEPTED | REJECTED` (B가 응답)
  - 같은 row에서 양측 상태를 모두 표현할 수 있어 별도 테이블 / 별도 컬럼 추가 불필요
  - 기존 `claimed = true` 대응 쿼리는 `claimStatus IN (CLAIMED, ACCEPTED, REJECTED)` 또는 목적별로 분기 (상세는 §4.6)

### 도메인 분리 판단

새 도메인을 만들지 않는다. 기존 `matching` 도메인 내부에서 처리한다.
- claim/accept/reject는 `MatchingResult` 애그리거트의 상태 전이 그 자체
- `feedback_no_new_domain.md` 정책 준수

### 1차 구현 범위

- `PATCH /api/matching-results/{id}/accept` — claim 받아주기
- `PATCH /api/matching-results/{id}/reject` — claim 거절하기
- `claimStatus` 전환 로직 + 상태 검증 (도메인 객체 내부)
- `/api/claimers/me` 쿼리에 `claimStatus = CLAIMED` 필터 추가 (이미 응답한 claim 은 목록에서 제외) — "상세 조회 쪽 영향" 참조

### 1차 범위 외 (후속)

- 응답 취소 (accept → claimed 되돌리기 등)
- 수락 시 X룸 자동 공유 / 알림 발행
- "거절" 당한 claim의 보존/제거 정책 (soft delete vs 이력 보존) — 본 계획은 상태만 유지
- 내가 응답했거나 응답받은 claim의 히스토리 조회 API

---

## 2. API 설계

api-todo.md 의 "매칭 > X 받아주기, 거절하기" 항목(현재 제목만)에 대응한다.

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| PATCH | `/api/matching-results/{matchingResultId}/accept` | 나를 claim한 상대 받아주기 | 필요 |
| PATCH | `/api/matching-results/{matchingResultId}/reject` | 나를 claim한 상대 거절하기 | 필요 |

### 2.1 URL 설계 근거

- 기존 `exclude/include/claim` 패턴과 동일: **상위 리소스 PATCH + 하위 동작 경로**
- RESTful URL 설계 원칙 — 리소스의 상태 변경이므로 PATCH
- 동작 경로 네이밍은 동사 그대로 (`accept`, `reject`). 동일 네이밍 컨벤션: `/claim`
- 요청 본문 없음 (path + auth 만으로 상태 결정 가능)

### 2.2 Request / Response

- **Path**: `matchingResultId` — `@DecryptId(ObfuscationType.MATCHING_RESULT)` 복호화
- **Auth**: `@AuthenticationPrincipal email: String` — claim 당한 당사자(B)임을 검증
- **Body**: 없음
- **Response**: 204 No Content (또는 200 OK) — 기존 exclude/include/claim 컨벤션과 통일

### 2.3 에러 케이스

| Status | 상황 |
|--------|------|
| 403 | 본인이 claim 당한 대상(target)이 아님 (`AccessDeniedException`) |
| 404 | matchingResultId 존재하지 않음 (`EntityNotFoundException`) |
| 409 | 현재 상태가 `CLAIMED`이 아님 — 이미 응답했거나 claim 전임 (`InvalidStateException`) |

---

## 3. 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                            │
│  MatchingResultCommandApi                                   │
│    ├── PATCH /{id}/accept → commandService.accept(id, email)│
│    └── PATCH /{id}/reject → commandService.reject(id, email)│
└────────────────────────────┬────────────────────────────────┘
                             │ (port)
┌────────────────────────────▼────────────────────────────────┐
│ domain/ma-domain-core                                       │
│  MatchingResultCommandService                               │
│    ├── accept(matchingResultId: Long, email: String)         │
│    └── reject(matchingResultId: Long, email: String)         │
│       1) findOne(id) → MatchingResult                        │
│       2) matchingResult.validateTargetOwnership(Email(email))│
│       3) matchingResult.accept() / reject()                  │
│       4) repo.updateClaimStatus(matchingResult)              │
│                                                              │
│  MatchingResult (도메인)                                     │
│    + claimStatus: ClaimStatus  (private set)                 │
│    + validateTargetOwnership(email): 대상 본인만 통과        │
│    + accept():  CLAIMED → ACCEPTED (아니면 InvalidState)     │
│    + reject():  CLAIMED → REJECTED (아니면 InvalidState)     │
│  ClaimStatus (enum): NONE / CLAIMED / ACCEPTED / REJECTED    │
│                                                              │
│  MatchingResultRepository (port)                            │
│    + updateClaimStatus(matchingResult)                       │
│    (기존 updateClaimed 는 제거 또는 위임으로 대체)           │
└────────────────────────────┬────────────────────────────────┘
                             │ (implements)
┌────────────────────────────▼────────────────────────────────┐
│ infrastructure/storage/ma-db-core                           │
│  MatchingResultTable                                        │
│    - claimed 컬럼 제거 또는 유지 후 점진 제거                │
│    + claimStatus = enumerationByName("CLAIM_STATUS", ...)    │
│  MatchingResultEntity                                       │
│    + claimStatus 필드 추가, toDomain/from 매핑 반영          │
│  MatchingResultCommandDao                                   │
│    + updateClaimStatus(matchingResult)                       │
│  MatchingResultQueryDao                                     │
│    · findClaimedByTarget: 조건 = CLAIM_STATUS='CLAIMED'      │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 변경 전략

### 4.1 상태 모델 (enum + 전이 규칙)

| 상태 | 의미 | 전이 가능 |
|------|------|-----------|
| `NONE` | 기본값 (claim 전) | → `CLAIMED` (A의 claim) |
| `CLAIMED` | A가 claim 했음 | → `ACCEPTED` (B 수락) / `REJECTED` (B 거절) |
| `ACCEPTED` | B가 받아줌 | 최종 상태 (본 계획에서 취소 없음) |
| `REJECTED` | B가 거절함 | 최종 상태 |

도메인 `MatchingResult` 에 전이 메서드를 둔다:
- `fun claim()` — 기존 메서드. 내부적으로 `claimStatus == NONE` 검증 후 `CLAIMED` 로 변경 (`validateNotClaimed` 로직 재작성)
- `fun accept()` — `claimStatus == CLAIMED` 검증 후 `ACCEPTED` 로 변경
- `fun reject()` — `claimStatus == CLAIMED` 검증 후 `REJECTED` 로 변경
- 검증은 도메인 내부에서 `InvalidStateException` 발생

### 4.2 소유권 검증 (중요)

기존 `validateOwnership(email)` 은 `registerEmail == email` 을 검증 (= A 기준). 본 기능은 **B 기준** 검증이 필요하므로 별도 메서드 추가:

- `fun validateTargetOwnership(email: Email)` 추가
  - `targetEmail != email` 이면 `AccessDeniedException(EntityType.MATCHING_RESULT, targetEmail.value, email.value)`
  - 이름은 "target 기준 소유권" 의미 (= claim 당한 당사자)

### 4.3 검증 책임 분리

| 검증 항목 | 위치 | 비고 |
|-----------|------|------|
| matchingResult 존재 | `MatchingResultRepository.findOne` | 없으면 `EntityNotFoundException` |
| 호출자 = targetEmail | `MatchingResult.validateTargetOwnership(Email)` | 도메인 내부 |
| 현재 상태 = CLAIMED | `MatchingResult.accept()` / `reject()` init | 도메인 내부 |
| 이중 응답 차단 | 상태 검증으로 자연히 차단 | `ACCEPTED`/`REJECTED` 는 `CLAIMED` 아님 |

Service 는 조합만:
1. `matchingResultRepository.findOne(id)` → `MatchingResult`
2. `matchingResult.validateTargetOwnership(Email(email))`
3. `matchingResult.accept()` 또는 `reject()`
4. `matchingResultRepository.updateClaimStatus(matchingResult)`

### 4.4 DB 스키마 변경

**MATCHING_RESULTS 테이블**:
- `CLAIMED BOOLEAN DEFAULT FALSE` → **제거**
- `CLAIM_STATUS VARCHAR(16) NOT NULL DEFAULT 'NONE'` **추가**
- 인덱스 교체:
  - `idx_matching_target_email_claimed (TARGET_EMAIL, CLAIMED)` → **제거**
  - `idx_matching_target_email_status (TARGET_EMAIL, CLAIM_STATUS)` **추가**

> 기존 데이터 마이그레이션이 운영에 필요하다면 별도 마이그레이션 스크립트로 처리 (개발 환경은 DDL 재적용 전제). 마이그레이션 규칙: `CLAIMED=true → CLAIM_STATUS='CLAIMED'`, 그 외 `'NONE'`.

### 4.5 Exposed 매핑

`MatchingResultTable`:
- `val claimStatus = enumerationByName("CLAIM_STATUS", 16, ClaimStatus::class).clientDefault { ClaimStatus.NONE }`
- 문자열로 저장 (숫자 저장은 enum 순서 변경 시 깨짐 — code-implementation-rules 지침)
- 기존 `claimed` 필드 제거

### 4.6 기존 쿼리 영향

**`MatchingResultQueryDao.findClaimedByTarget(email)`** — `/api/claimers/me` 가 사용하는 쿼리
- 기존: `targetEmail eq email AND claimed eq true`
- 변경: `targetEmail eq email AND claimStatus eq ClaimStatus.CLAIMED`
- **의미 차이**: 이미 `ACCEPTED`/`REJECTED` 한 claim 은 목록에서 제외된다. 이는 의도된 동작 (UX 기대: 내가 응답해야 할 claim 만 노출)

**`NewMatchingResult` 및 `MatchingResultCommandDao.saveAll`**
- 기존 `this[MatchingResultTable.claimed] = false` 제거
- `this[MatchingResultTable.claimStatus] = ClaimStatus.NONE` 으로 대체 (또는 `clientDefault` 에 의존)

**기타 참조**
- `MatchingResult.claimed: Boolean` 프로퍼티 → `claimStatus: ClaimStatus` 로 대체
- 외부에서 `matchingResult.claimed` 를 읽던 곳(예: Response)이 있다면 `claimStatus` 로 교체. 그리드에 이미 표현되던 호환을 유지하려면 `MatchingResult.isClaimed(): Boolean = claimStatus != ClaimStatus.NONE` 같은 파생 메서드 추가 고려.

### 4.7 포트 변경

`MatchingResultRepository`:
- 제거: `fun updateClaimed(matchingResult: MatchingResult)`
- 추가: `fun updateClaimStatus(matchingResult: MatchingResult)`
- 나머지 시그니처 유지 (`findClaimedByTarget(Email): List<MatchingResult>` — 내부 쿼리만 변경)

### 4.8 Controller 레이어

`MatchingResultCommandApi` 에 엔드포인트 2개 추가 (기존 `claim`, `exclude`, `include` 옆).
- `@PatchMapping("/{matchingResultId}/accept") fun accept(...)`
- `@PatchMapping("/{matchingResultId}/reject") fun reject(...)`
- 파라미터: `@AuthenticationPrincipal email: String`, `@PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long`
- 반환: `Unit` (Spring 기본 200 OK) — 기존 컨벤션과 동일

### 4.9 api-todo.md 정리

`📋 TODO > 매칭 > X 받아주기, 거절하기` 항목 제거 및 `✅ 완료된 API > 매칭` 표에 다음 두 줄 추가:

| PATCH | /api/matching-results/{id}/accept | 나를 claim한 상대 받아주기 |
| PATCH | /api/matching-results/{id}/reject | 나를 claim한 상대 거절하기 |

`feedback_api_todo_keep_domain.md` 에 따라 매칭 섹션은 유지하고, 해당 서브 섹션이 비게 되면 "작업할 내용 없음" 으로 표시.

---

## 5. 변경 파일 목록

### Phase 1: DDL

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `infrastructure/.../script/ddl.sql` | 수정 | `CLAIMED` 컬럼 제거, `CLAIM_STATUS VARCHAR(16) NOT NULL DEFAULT 'NONE'` 추가. 인덱스 `idx_matching_target_email_claimed` 제거, `idx_matching_target_email_status (TARGET_EMAIL, CLAIM_STATUS)` 추가 |

### Phase 2: Domain Model

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 2 | `domain/.../matching/domain/ClaimStatus.kt` | 신규 | enum: `NONE, CLAIMED, ACCEPTED, REJECTED` |
| 3 | `domain/.../matching/domain/MatchingResult.kt` | 수정 | `claimed: Boolean` → `claimStatus: ClaimStatus` 교체. `claim()` 재작성(`NONE → CLAIMED`, 아니면 `InvalidStateException`). `accept()`, `reject()` 추가 (전이 검증 포함). `validateTargetOwnership(email)` 추가. `validateNotClaimed()` 제거(상태 검증으로 흡수) |

### Phase 3: Domain Port

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 4 | `domain/.../matching/domain/port/MatchingResultRepository.kt` | 수정 | `updateClaimed` 제거, `fun updateClaimStatus(matchingResult: MatchingResult)` 추가 |

### Phase 4: Application Service

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 5 | `domain/.../matching/application/MatchingResultCommandService.kt` | 수정 | `claim()` 내부의 `updateClaimed` 호출을 `updateClaimStatus` 로 교체. `fun accept(matchingResultId: Long, email: String)` / `fun reject(matchingResultId: Long, email: String)` 추가 — 흐름: findOne → validateTargetOwnership → accept/reject → updateClaimStatus |

### Phase 5: Infrastructure

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 6 | `infrastructure/.../entity/table/MatchingResultTable.kt` | 수정 | `claimed` 컬럼 제거. `claimStatus = enumerationByName("CLAIM_STATUS", 16, ClaimStatus::class).clientDefault { ClaimStatus.NONE }` 추가 |
| 7 | `infrastructure/.../entity/MatchingResultEntity.kt` | 수정 | `claimed: Boolean` → `claimStatus: ClaimStatus`. `toDomain()` / `from(row)` 매핑 반영 |
| 8 | `infrastructure/.../dao/MatchingResultCommandDao.kt` | 수정 | `updateClaimed` → `updateClaimStatus(matchingResult: MatchingResult)` 로 이름/구현 변경 (`it[claimStatus] = matchingResult.claimStatus`). `saveAll` 의 `this[claimed] = false` 제거 (clientDefault 로 충분) |
| 9 | `infrastructure/.../dao/MatchingResultQueryDao.kt` | 수정 | `findClaimedByTarget` 조건을 `claimed eq true` → `claimStatus eq ClaimStatus.CLAIMED` 로 변경 |
| 10 | `infrastructure/.../repository/MatchingResultCoreRepository.kt` | 수정 | `override fun updateClaimed` → `override fun updateClaimStatus` 위임으로 변경 |

### Phase 6: Boot - API Layer

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 11 | `boot/.../matching/api/MatchingResultCommandApi.kt` | 수정 | `PATCH /{matchingResultId}/accept`, `PATCH /{matchingResultId}/reject` 엔드포인트 2개 추가 |
| 12 | `boot/.../matching/api/response/MatchingResultResponse.kt` | 수정 (영향 시) | 기존 `claimed: Boolean` 필드가 있다면 `claimStatus: ClaimStatus` 또는 `accepted/rejected` 플래그 조합으로 재설계. 상세는 해당 파일 확인 후 결정 |

### Phase 7: Test

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 13 | `domain/.../test/.../matching/domain/MatchingResultTest.kt` | 수정 | `claim()` 전이 검증, `accept()` / `reject()` 정상 전이, `CLAIMED` 가 아닌 상태에서 accept/reject 호출 시 예외, `validateTargetOwnership` 실패/성공 |
| 14 | `domain/.../test/.../matching/application/MatchingResultCommandServiceTest.kt` | 수정 | accept / reject 흐름 (소유권 위반, 상태 위반, 정상 케이스) |
| 15 | `infrastructure/.../test/.../matching/dao/MatchingResultCommandDaoTest.kt` | 수정 | `updateClaimStatus` 값이 DB 에 VARCHAR 로 저장되는지, enum 매핑 정확한지 |
| 16 | `infrastructure/.../test/.../matching/dao/MatchingResultQueryDaoTest.kt` | 수정 | `findClaimedByTarget` 이 `CLAIMED` 만 반환 (ACCEPTED/REJECTED 제외) |
| 17 | `boot/.../test/.../matching/api/MatchingResultCommandApiTest.kt` | 수정 | REST Docs: accept/reject 엔드포인트 2개. 상태 위반 케이스(이미 응답됨) 실패 테스트 |

### Phase 8: Docs / 정리

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 18 | `docs/api-todo.md` | 수정 | 매칭 섹션에서 "X 받아주기, 거절하기" 제거, 완료 표에 2행 추가. 매칭 하위에 남은 TODO 없으면 "작업할 내용 없음" 표기 |

---

## 6. 고려사항

- **상태 모델 선택 근거**: `claimed: Boolean` 은 이미 서비스 전역에서 의미 변질이 시작되는 상태(= `acceptedOrRejected` 까지 같은 true 로 묶어야 할 필요). enum 단일 컬럼이 "한 claim 에 대한 단일 라이프사이클" 을 표현하기에 가장 적합하다. 두 개의 boolean(`accepted`, `rejected`) 조합은 불가능한 상태(두 개 모두 true) 를 만들 수 있어 배제.
- **FK 안전성**: 컬럼 제거/추가만, 다른 테이블이 `CLAIMED` 를 참조하지 않으므로 FK 이슈 없음 (FK 미사용 원칙 준수, `feedback_no_fk.md`).
- **인덱스 변경**: `/api/claimers/me` 핵심 쿼리가 `(TARGET_EMAIL, CLAIM_STATUS)` 로 매칭됨. 기존 복합 인덱스 `(TARGET_EMAIL, CLAIMED)` 와 선택도 차이는 미미하지만, VARCHAR 인덱스는 BOOLEAN 대비 약간 더 크다. 대안은 enum 을 INT 로 저장하는 것이지만 순서 변경 리스크 대비 가치 낮음 → VARCHAR 유지.
- **`findClaimedByTarget` 의미 변경**: `/api/claimers/me` 에서 이미 응답한 claim 은 제외된다. 기존 동작과 달라지므로 프론트 협의 필요 (의도된 UX 라면 추가 작업 없음). 이력 조회는 후속 API 로 분리.
- **동시성**: 동일 claim 에 대한 accept 와 reject 동시 호출은 DB 의 최종 UPDATE 기준으로 결정됨. 현재 범위에서 락 도입 불필요(희소 케이스 + 최종 상태가 둘 중 하나이면 충분). 엄격 보장이 필요하면 후속으로 `SELECT ... FOR UPDATE` 도입 가능.
- **A의 claim 취소 (unclaim)**: 본 계획에 없음. 필요 시 `CLAIMED → NONE` 전이와 `/claim` 취소 엔드포인트를 별도 plan 으로 설계. 단, `ACCEPTED`/`REJECTED` 상태에서의 취소는 의미/정책 정의 필요.
- **알림/이벤트 확장점**: `accept()` 호출 성공 직후 Service 단에서 `ApplicationEventPublisher` 로 `ClaimAcceptedEvent` 발행 가능. X룸 공유/알림은 해당 이벤트 리스너에서 처리. 본 계획에 포함하지 않되, Service 메서드가 단일 책임을 유지하도록 설계.
- **Response 에 `claimStatus` 노출 여부**: 외부 API 계약에 `claimed: Boolean` 이 이미 노출되어 있다면, 호환성 위해 `claimStatus` 와 `claimed`(= `claimStatus != NONE`) 둘 다 내보내는 것도 가능. 깔끔한 방향은 `claimStatus` 만 노출하고 프론트 전환.
- **도메인 객체 변환 위치**: Request 에서 `Email()` 을 생성하지 않고 Controller 는 raw `String email` 만 Service 에 전달. Service 가 내부에서 `Email(email)` 로 감싼다 (`feedback_domain_object_in_domain.md`). 현재 `MatchingResultCommandService.claim` 패턴 그대로.
- **테스트 인증**: API 테스트는 `BaseApiTest` 에 포함된 `@WithAuthMember` 기본값(holeman@naver.com) 사용. 중복 선언 금지 (`feedback_no_duplicate_with_auth_member.md`). target 본인이 아닌 케이스 검증은 fixture 에서 `targetEmail` 을 기본 인증 이메일과 일치시키거나 다른 이메일로 오버라이드.

---

## 7. 검증 항목

- [ ] DDL: `CLAIM_STATUS` 컬럼 생성 / `CLAIMED` 제거 / 인덱스 교체
- [ ] `ClaimStatus` enum 정의 + `MatchingResult.claim/accept/reject` 전이 단위 테스트 (유효 전이 + 불법 전이 예외)
- [ ] `validateTargetOwnership`: target 본인 통과 / 타인 `AccessDeniedException`
- [ ] `MatchingResultCommandService.accept` 정상 흐름: findOne → target 검증 → 상태 전이 → updateClaimStatus
- [ ] `reject` 동일
- [ ] `findClaimedByTarget` 결과에서 `ACCEPTED`/`REJECTED` 제외됨 (DAO 통합 테스트)
- [ ] REST Docs: `PATCH /accept`, `PATCH /reject` snippet 생성. 실패 케이스(잘못된 상태, 권한 없음) 문서화
- [ ] `./gradlew build` 성공, 기존 matching 관련 테스트 회귀 통과
- [ ] api-todo.md 갱신 (완료 표 2행 추가, TODO 제거)

---

## 📋 스킬 적용 체크리스트

### plan-writing 스킬
- [x] SKILL.md 파일을 Read로 읽었는가 (requirement-planning 로드됨)
- [x] 코드 스니펫 없이 시그니처/설명 수준으로 작성했는가
- [x] 변경 전략을 테이블로 정리했는가
- [x] Phase 별 변경 파일 목록을 한 줄 요약으로 작성했는가
- [x] `docs/plan/{YYYYMM}/` 디렉토리에 저장 (`docs/plan/202604/`)
- [x] 고려사항(상태 모델 선택 근거, 인덱스, 동시성, 확장점 등) 포함

### code-implementation-rules 스킬
- [x] Service 는 조합만 담당, 상태 검증은 도메인 객체 내부
- [x] 도메인 객체에 행위 부여 (`MatchingResult.accept/reject/validateTargetOwnership`)
- [x] 포트 인터페이스가 도메인 객체 사용 (`updateClaimStatus(matchingResult)`)
- [x] FK 미사용 (컬럼/인덱스만)
- [x] 도메인 객체 변환은 도메인 모듈 내부 (Request 에서 Email 생성 금지)
- [x] RESTful URL: PATCH + 하위 동작 경로 (기존 claim/exclude/include 패턴 일관)
