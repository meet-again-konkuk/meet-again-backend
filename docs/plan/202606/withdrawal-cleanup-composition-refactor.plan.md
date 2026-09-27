# Plan: 탈퇴 정리 Writer 단순화 — 상속·도메인별 클래스 8개 제거

- 작성일: 2026-06-12
- 작업 유형: 리팩토링 (구조 단순화, 동작 변경 없음)
- 브랜치: `feat/withdrawal-anonymize-batch` (또는 후속 `refactor/withdrawal-cleanup-simplify`)

## ⏸ 진행 현황 (2026-06-13, 다음 세션 이어가기용)

**Phase 1 (본 plan 본문 = 상속→합성) — ✅ 구현 완료, git 미커밋 (working tree에 존재).**
- 신규 `MemberWithdrawalCleaner`(@Component, 8개 `cleanXxx`/`anonymizeMember` 메서드 + repo 13개 의존성 응집).
- `MemberCleanupItemWriter` → `(Member)->Unit` 받는 generic writer.
- `MemberWithdrawalCompleteJobConfig` → 의존성 cleaner 1개로 축소, step writer `MemberCleanupItemWriter(cleaner::cleanXxx)` 와이어링.
- `Member.withdrawnEmail()` 도메인 행위 추가.
- 8개 `XxxCleanupItemWriter` git rm. 컴파일·회귀(`:boot:ma-boot-batch:test`,`:ma-db-core:test`) 통과.

**같은 working tree에 함께 있는 (이 작업 이전) 미커밋 변경분** (참고 — 별도 작업이지만 아직 커밋 안 됨):
- 매칭 상대노출 보존: `MatchingResultRepository.deleteByRegister`+`anonymizeTarget`(port/dao/repo/test), `MatchingCleanupItemWriter`(→ 이제 cleaner.cleanMatching로 흡수됨).
- `jobStartTime` → `DateJobParameter` 교체(테스트/지정일 삭제 가능), `@Qualifier` 정리.
- 신규 문서: `docs/member-withdrawal-cleanup.md`(삭제 vs 익명화 기준표), `docs/frontend-discussion.md` 2번(isWithdrawn 표기).

**Phase 2 — ✅ 구현 완료 (2026-06-13), git 미커밋.** read 중복 제거: 8 step → 1 step.
- `MemberWithdrawalCleaner.clean(member)` 추가(8개 순서대로 호출, anonymize 마지막). 기존 8개 `cleanXxx`/`anonymizeMember`는 `clean`만 호출하므로 **전부 `private`로 전환**(§2-1 최소 가시성).
- `MemberWithdrawalCompleteJobConfig`: 8 step + 8 reader → **`memberWithdrawalCleanupStep` 1개 + `memberWithdrawalCleanupReader` 1개 + writer `MemberCleanupItemWriter(cleaner::clean)`**. job은 단일 step만 `.start()`. `buildCleanupStep` 헬퍼 인라인 제거. reader 빈이 1개뿐이라 `@Qualifier` 제거(타입 기반 주입).
- **페이지 크기 20으로 축소**(사용자 결정): `AbstractJobConfig.CHUNK_SIZE_20` 신설, reader pageSize=20. 한 트랜잭션 = 회원 20명 × 8도메인. 롤백 blast radius 1/5.
- 컴파일·회귀(`:boot:ma-boot-batch:test`, `:ma-db-core:test`) 통과.

## 요구사항 요약

`memberWithdrawalCompleteJob`의 정리 Writer 8종이 `abstract class MemberCleanupItemWriter`를 상속하는데, 두 문제가 있다.

1. **어색한 상속**: `CommunityCleanupItemWriter : MemberCleanupItemWriter`는 개념적 "~의 일종(is-a)"이 아니라 알고리즘 골격 2줄 재사용(Template Method 패턴)일 뿐이다.
2. **클래스 과잉**: Writer 8개 중 **5개가 repository 호출 딱 한 줄**(auth/xroom/support/photo/anonymize)이다. 한 줄 위임을 위해 클래스를 두는 건 ceremony다. 나머지 3개(point/matching/community)만 여러 repository를 조율한다.

→ **단일 추상 메서드(SAM, Single Abstract Method) 인터페이스를 람다/함수참조로 넘기는 방식**으로 바꿔, Writer 8개 + 추상 1개를 **generic Writer 1개**로 합치고, 정리 로직은 **단일 클래스 1개에 메서드로 모은다.**

> 동작(도메인별 삭제/익명화 결과·순서·소프트삭제 여부)은 **그대로 유지**한다. 기준표는 [[member-withdrawal-cleanup]] 참조.

## 현행 vs 목표

| 구분 | 현행 | 목표 |
|------|------|------|
| Writer | `abstract MemberCleanupItemWriter` + 8 서브클래스 | generic `MemberCleanupItemWriter` **1개** (정리 동작을 `(Member) -> Unit` 함수로 주입) |
| 정리 로직 | 8개 파일에 분산 | `MemberWithdrawalCleaner` **1개** 컴포넌트에 도메인별 메서드로 응집 |
| 관계 | 상속(is-a) | Writer가 함수를 받음(합성) / JobConfig는 와이어링만 |
| 익명 이메일 | Writer에서 `Email.withdrawn(member.id)` 조립 | `member.withdrawnEmail()` 도메인 행위 |
| JobConfig 의존성 | repository 13개 + photoCleaner 등 ~16개 | `MemberWithdrawalCleaner` + reader용 2개 = **3개로 축소** |

**클래스 수: 9개(추상1+writer8) → 2개(generic writer1 + cleaner1).** JobConfig도 대폭 슬림화.

## 핵심 설계

### generic Writer (단일)
- `class MemberCleanupItemWriter(private val clean: (Member) -> Unit) : ItemWriter<List<Member>>`
- `write(chunk)`는 `chunk.items.flatten().forEach(clean)`만 수행. 추상 메서드 없음, 상속 없음.
- 별도 인터페이스 타입은 만들지 않고 코틀린 함수 타입 `(Member) -> Unit`을 직접 사용(타입 증가 0). 호출부에서 람다 또는 **함수 참조**(`cleaner::cleanAuth`)를 넘긴다.

### 정리 로직 단일 클래스
- `MemberWithdrawalCleaner` (`@Component`, 배치 모듈) — 현재 8개 Writer가 흩어 들고 있던 repository 의존성을 **여기로 모은다.**
- 도메인별 메서드(현행 각 Writer의 `cleanup` 본문을 그대로 옮김):
  - `cleanAuth(member)` — refresh token 삭제
  - `cleanMatching(member)` — 타겟정보 삭제 + 매칭 register 삭제 + target 익명화
  - `cleanPoint(member)` — 포인트 잔액 삭제 + 이력 익명화
  - `cleanCommunity(member)` — 글·댓글 익명화 + 좋아요 삭제
  - `cleanSupport(member)` — 문의 익명화
  - `cleanXroom(member)` — xroom 삭제
  - `cleanPhoto(member)` — 사진 정리(도메인 `MemberPhotoCleaner` 위임)
  - `anonymizeMember(member)` — 회원 본체 익명화+soft delete
- 각 메서드는 함수 참조로 Writer에 주입된다: `MemberCleanupItemWriter(cleaner::cleanAuth)`.

> 단일 호출 5개도 메서드로 두는 이유: JobConfig에 repository를 다시 주입하지 않고 cleaner 한 곳을 거치게 해서 **JobConfig를 순수 와이어링으로 유지**(이름 있는 testable 표면도 확보). 사용자 지적("클래스 8개 과잉")은 8개를 **메서드 8개(클래스 1개)**로 합쳐 해소.

## 영향 범위

| 파일 | 변경 | 수준 |
|------|------|------|
| `domain/.../member/domain/Member.kt` | `fun withdrawnEmail(): Email = Email.withdrawn(id)` 추가, `anonymize()` 내부도 재사용 | 수정 |
| `.../member/MemberCleanupItemWriter.kt` | `abstract class`(템플릿) → `class`(`(Member)->Unit` 주입받는 generic writer) | 수정 |
| `.../member/MemberWithdrawalCleaner.kt` | 8개 도메인 메서드 + repository 의존성 보유. 본문은 기존 8개 Writer 내용 이관 | 신규 |
| `.../member/AuthCleanupItemWriter.kt` 외 7개 | 삭제(로직은 cleaner 메서드로 이관) | 삭제(git rm) |
| `.../member/MemberWithdrawalCompleteJobConfig.kt` | repository 13개 의존성 제거 → `MemberWithdrawalCleaner` 주입. 각 step writer를 `MemberCleanupItemWriter(cleaner::cleanXxx)`로 와이어링 | 수정 |

### 와이어링 (변경 후, 예시)
```
buildCleanupStep("authCleanupStep", reader, MemberCleanupItemWriter(cleaner::cleanAuth))
buildCleanupStep("communityCleanupStep", reader, MemberCleanupItemWriter(cleaner::cleanCommunity))
```
step 이름·순서·reader·트랜잭션 경계·청크 사이즈 전부 동일. `buildCleanupStep`의 `writer: MemberCleanupItemWriter` 타입도 유지.

## 결정사항

### D-1. 정리 로직 위치 → **단일 `MemberWithdrawalCleaner` 컴포넌트 (권장)**
- 대안(JobConfig에 람다/private 함수 인라인)은 클래스를 0개로 만들지만, JobConfig가 repository 13개를 계속 들고 **정리 로직까지 떠안아 비대**해진다(SRP 악화).
- 단일 cleaner로 모으면: 8개 클래스 제거 + JobConfig 슬림화 + "탈퇴 정리 정책"의 응집된 단일 출처 확보. → 이쪽 채택.

### D-2. SAM 타입 → **코틀린 함수 타입 `(Member) -> Unit` 직접 사용**
- 별도 `fun interface MemberCleaner` 선언 안 함(타입 증가 0). 가독성 위해 `typealias`가 필요하면 후속 고려.

### D-3. `clean`/메서드 입력 → **`Member` 전체 유지**
- `anonymizeMember`가 회원 전체(gender·withdrawalRequestedAt 등)를 쓰고 reader가 `List<Member>`를 생산하므로 입력 축소는 범위 밖. `withdrawnEmail()` 추출로 id 직접 접근만 제거.

### D-4. 단일 호출 5개도 cleaner 메서드로 둠
- JobConfig가 repository를 다시 주입하지 않도록 모든 도메인을 cleaner 경유. 클래스가 아니라 메서드라 과잉 아님.

## 구현 순서

1. (도메인) `Member.withdrawnEmail()` 추가, `anonymize()`에서 재사용.
2. (배치) `MemberWithdrawalCleaner` 신설 — repository 의존성 이관 + 8개 메서드(기존 Writer 본문 그대로, `Email.withdrawn(member.id)` → `member.withdrawnEmail()`).
3. (배치) `MemberCleanupItemWriter`를 `(Member)->Unit` 주입 generic writer로 전환.
4. (배치) 8개 `XxxCleanupItemWriter` 삭제(git rm).
5. (배치) `MemberWithdrawalCompleteJobConfig` — 의존성 교체 + step writer를 `MemberCleanupItemWriter(cleaner::cleanXxx)`로.
6. 컴파일(`:boot:ma-boot-batch`, `:domain:ma-domain-core`) + 회귀(`:boot:ma-boot-batch:test`, `:infrastructure:storage:ma-db-core:test`).

## 삭제 대상

| 대상 | 사유 |
|------|------|
| `abstract` 템플릿(`MemberCleanupItemWriter`의 상속 구조) | generic writer로 대체 |
| 8개 `XxxCleanupItemWriter` | 로직을 `MemberWithdrawalCleaner` 메서드로 이관 |
| Writer 내 `Email.withdrawn(member.id)` | `member.withdrawnEmail()`로 대체 |

## 리스크 및 주의

- **동작 불변이 절대 조건**: 각 `cleanXxx` 메서드 본문은 대응 Writer와 **호출 내용 동일**해야 함(메서드명·위치만 변경). [[member-withdrawal-cleanup]] 기준표와 대조 검증.
- **함수 참조 바인딩**: `cleaner::cleanAuth`는 `(Member)->Unit`과 시그니처 일치해야 함(모든 메서드 `fun cleanXxx(member: Member)` 단일 파라미터로 통일).
- **삭제는 git rm**: Write 후 수동 삭제 금지([[feedback_git_mv]] 취지 — 버전 관리 정합).
- **Member 과잉 전달·step당 reader 8회 재조회**는 본 범위 밖(구조 단순화만, 성능/입력최소화 별건).
- **photo 네이밍**: 도메인 컴포넌트 `MemberPhotoCleaner`(domain/.../photo/)는 그대로 두고, 메서드명만 `cleanPhoto`로 위임.

## 테스트 전략

- 기존 cleanup writer/job 테스트 **없음** → 깨질 테스트 없음.
- (선택) `MemberWithdrawalCleanerTest` — 각 `cleanXxx(member)` 호출 시 기대 repository 메서드 호출 검증(Mockk verify). 구현 안정 후 작성([[feedback_impl_then_test]]).
- (선택) `Member.withdrawnEmail()` 값(`withdrawn_{id}@deleted.local`) 검증.

## 구현 시 참조 (규칙 본문 복제 안 함)

- 합성 우선·도메인 행위 위임·포트 규칙: [[code-implementation-rules]]
- 모듈·패키지 배치: [[clean-architecture]]
- 네이밍/가독성: [[clean-code]]
- 테스트(KoTest/Mockk): [[kotest-writing]]
- 도메인별 삭제/익명화 기준(동작 불변 기준): [[member-withdrawal-cleanup]]

---

# 후속 페이즈 (Phase 2) — read 중복 제거: 8 step → 1 step

> 상태: **사용자 합의 완료, 다음 세션에서 구현.** Phase 1(상속→합성) 완료를 전제로 한다.

## 문제

`memberWithdrawalCompleteJob`이 step 8개를 갖고, 각 step이 `@StepScope` reader를 따로 들어 **같은 만료 회원 집합을 8번 페이징 조회**한다. reader 쿼리:
`MemberQueryDao.findExpiredWithdrawalRequests` = `activeRows(deleted=false) AND withdrawalRequestedAt 비null AND ≤ cutoff`, id 커서 ASC, `limit(pageSize=100)`.

두 문제:
1. **중복 read 8회** (대상은 정해져 있는데 step마다 재조회).
2. **부분 정리 상태 창**: step 사이에 "일부 도메인만 정리된" 중간 상태 존재.

## 목표 구조

8개 step·8개 reader → **단일 step + 단일 reader + 단일 writer**. 한 회원에 8개 정리를 한 트랜잭션에서 수행.

- `MemberWithdrawalCleaner`에 `fun clean(member: Member)` 추가 — 기존 step 순서 그대로 호출:
  `cleanAuth → cleanMatching → cleanPoint → cleanCommunity → cleanSupport → cleanXroom → cleanPhoto → anonymizeMember` (anonymize 마지막).
- `MemberWithdrawalCompleteJobConfig`:
  - step 8개(`authCleanupStep`…`memberAnonymizeStep`) + reader 8개(`authCleanupReader`…) → **`memberWithdrawalCleanupStep` 1개 + reader 1개**로 통합.
  - `memberWithdrawalCompleteJob`은 단일 step만 `.start(...)`.
  - writer = `MemberCleanupItemWriter(cleaner::clean)`.
  - `buildCleanupStep` 헬퍼는 단일 호출이 되므로 인라인 정리 가능.

## 효과 / 트레이드오프

- read 8회 → **1회**, 회원 단위 **원자적 정리**(부분 상태 창 제거), JobConfig 대폭 축소.
- 재시작 멱등성 유지(`deleted=false` 필터 + id 커서 → 이미 익명화된 회원 자동 제외).
- **상실**: 도메인별 step restart(실질 손해 적음 — 필터로 스킵). **트랜잭션 커짐**: 1페이지×8도메인.

## 미결정 (구현 전 확정 필요)

- **청크/페이지 크기**: 현재 페이지 100 유지 vs 축소(예: 20~50)로 트랜잭션 부담 완화. 만료 집합 규모 보고 결정.
- (선택) 회원 단위 트랜잭션을 더 잘게 가져갈지(reader가 `Member` 단건 반환 + chunk N) — 더 큰 변경이라 별건 후보.

## 동작 보존 검증
- 8개 정리 호출 내용·순서가 Phase 1과 동일해야 함([[member-withdrawal-cleanup]] 기준표 대조).
- 단일 step에서 `deleted=false` 필터 + 익명화 동시 진행해도 id 커서가 단조 증가라 재읽기/무한루프 없음(확인 완료).
