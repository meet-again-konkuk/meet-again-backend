# Plan: 탈퇴 백업 읽기 벌크화 (per-member collect → 청크 단위 collect)

- 작성일: 2026-06-14
- 작업 유형: 성능 개선 (백업 수집 쿼리 fan-out 축소)
- 브랜치: `feat/withdrawal-anonymize-batch` (working tree, 미커밋 상태 위에서 진행)
- 선행: 탈퇴 데이터 백업(R/P/W) 구현 완료 전제. 본 plan은 그 위 읽기 최적화.

## 배경 / 문제

현재 백업은 Processor에서 회원 1명씩 `MemberBackupArchiver.archive(member)` → `collector.collect(member)`가 **회원당 조회 12쿼리**(targetInfo·matching×2·point잔액·pointHistory·post·comment·postLike·commentLike·inquiry·xroom·photo)를 수행. 청크 20명이면 **~240 쿼리/청크**.

→ 청크 회원들을 **email 집합으로 한 번에 조회**(`find(emails)`)하고 **메모리에서 회원별 그룹핑**하면 쿼리가 **12×N → ~12/청크**로 줄어든다([[code-implementation-rules]] §18: 벌크 조회 후 메모리 처리, N+1 방지).

> **백업 파일은 per-member(`{memberId}.json`) 유지** — 조회성·멱등 보존. 줄이는 건 *읽기 쿼리 수*지 업로드 수가 아니다(업로드 배칭은 별건, 미채택).

## 핵심 비용: set 기반 조회 포트 추가

collect가 회원별 단건 조회 대신 집합 조회를 쓰도록 도메인별 `find(emails: Set<Email>): List<X>`를 보강한다. (반환 List를 collector가 email로 groupBy.)

| 소스 | 현재 | 조치 | 사유 |
|------|------|------|------|
| MemberPhoto | `find(emails: Set<Email>)` | **그대로 재사용** | 이미 집합 조회 존재 |
| TargetInfo | `find(email)` | set 변형 **추가** | `TargetInfoQueryService`가 단건 사용 → 단건 유지 |
| MatchingResult(register) | `find(email, excluded)` | set 변형 **추가** | `MatchingResultQueryService` 사용 → 단건 유지 |
| MatchingResult(claimed) | `findClaimedByTarget(email)` | set 변형 **추가** | `MatchingResultQueryService` 사용 → 단건 유지 |
| MemberPoint | `findOneOrInitial(email)` | `find(emails): List<MemberPoint>` **추가** | `PointChargeService` 사용 → 단건 유지. 행 없는 회원은 그룹핑 시 initial 적용 |
| PointHistory | `find(email)` | set로 **교체**(단건 제거 가능) | collector 전용(이번 세션 추가) |
| Post | `find(email)` | set로 **교체** | collector 전용 |
| Comment | `find(email)` | set로 **교체** | collector 전용 |
| PostLike | `find(email)` | set로 **교체** | collector 전용 |
| CommentLike | `find(email)` | set로 **교체** | collector 전용 |
| Inquiry | `find(email)` | set로 **교체** | collector 전용 |
| Xroom | `find(email)` | set로 **교체** | collector 전용 |

→ **신규 set 조회 ~10종** (각 포트 + DAO + Repository). DAO는 `Table.activeRows { emailCol inList emails }` 후 매핑. 빈 집합이면 즉시 `emptyList()`(§18, 불필요 쿼리 회피).
**§13 네이밍**: `find(emails: Set<Email>)` (단건 `find(email)`과 파라미터 타입으로 구분되는 오버로드).

## 구조 변경 (벌크는 청크 단위 ⇒ Spring Batch 영향)

읽기 벌크는 "청크 N명을 한꺼번에" 조회해야 하므로 **청크 단위 훅**이 필요하다. Spring Batch의 청크 단위 지점은 **Writer**다(`ItemProcessor`는 태생이 per-item). 따라서 백업 수집이 per-item Processor에 남을 수 없다.

### Plan A (권장) — 단건 Reader 유지 + Composite Writer
- Reader `ExpiredWithdrawalMemberItemReader: ItemReader<Member>` — **변경 없음**.
- **Processor 제거** (per-item 백업이 사라짐; 벌크 수집은 per-item 불가).
- Writer = `CompositeItemWriter<Member>` 2개 위임:
  1. `MemberBackupItemWriter` — `archiver.archive(chunk.items)` (벌크 수집→직렬화→per-member 저장)
  2. `MemberPurgeItemWriter` — `dataCleaner.clean` 으로 청크 정리 (기존)
- "백업 후 삭제" 순서 = Composite 위임 순서로 보장. 청크 `CHUNK_SIZE_20`.
- 장점: 단건 Reader 보존, Writer 2개 각 단일책임. 단점: R/P/**W** 에서 P 역할이 빠짐.

### Plan B (대안) — 페이지 Reader + 벌크 Processor (R/P/W 3역할 유지)
- Reader `ItemReader<List<Member>>` (페이지=N 반환), 청크 1.
- Processor `<List<Member>, List<MemberBackupBundle>>` — 벌크 수집 + 스냅샷 빌드(순수 읽기 변환). **읽기 벌크가 여기서 일어남.**
- Writer `<List<MemberBackupBundle>>` = Composite[backup 저장, purge].
- 장점: Processor 살아있음(벌크 변환). 단점: item이 단건 Member가 아니라 페이지 List로 회귀(직전에 단건으로 바꾼 걸 되돌림).

> 트레이드오프: "단건 R/P/W"(직전 결정)와 "읽기 벌크"는 상충. Plan A는 단건 Reader를 지키고 Processor를 버림, Plan B는 Processor를 지키고 단건 Reader를 버림. **권장 = Plan A** (단건 Reader 보존 + Composite Writer가 더 단순).

## 도메인 컴포넌트 시그니처 변경

| 구성요소 | 현재 | 변경 |
|---|---|---|
| `MemberWithdrawalBackupCollector` | `collect(member): MemberWithdrawalBackup` | `collect(members: List<Member>): List<MemberWithdrawalBackup>` — 집합 조회 후 email로 groupBy하여 회원별 스냅샷 빌드 |
| `MemberBackupArchiver` | `archive(member)` | `archive(members: List<Member>)` — `collector.collect(members)` 1회(벌크) → 각 backup serialize+store(per-member 파일) |

- 그룹핑: 각 집합 조회 결과 `List<X>`를 `groupBy { it.<emailField> }` → `Map<Email, List<X>>`. 회원별로 lookup(없으면 빈 리스트/`MemberPoint` initial).
- 마스킹·`MemberBackupView.from(member)`·직렬화·저장 포트·키 규칙(`withdrawal-backup/{memberId}.json`) **불변**.

## 영향 범위 (파일)

### domain (포트)
- 신규 set 메서드: `TargetInfoQueryRepository`, `MatchingResultRepository`(find+findClaimedByTarget), `MemberPointRepository`, `PointHistoryRepository`, `PostQueryRepository`, `CommentQueryRepository`, `PostLikeRepository`, `CommentLikeRepository`, `InquiryQueryRepository`, `XroomQueryRepository`
- `MemberWithdrawalBackupCollector`(List 입력으로), `MemberBackupArchiver`(List 입력으로)

### infrastructure/storage/ma-db-core
- 각 DAO에 `find(emails: Set<String>)`(`inList`) + Repository 매핑. collector 전용 7종은 단건 메서드 제거 가능, 4종은 단건 유지+추가.

### boot/ma-boot-batch
- `MemberBackupItemProcessor` **삭제**(Plan A) → `MemberBackupItemWriter` 신설.
- `MemberWithdrawalCompleteJobConfig` — step을 `.processor(...)` 제거하고 `.writer(CompositeItemWriter[backup, purge])`로. (Plan B면 Reader/Processor 타입 변경.)

## 구현 순서 (Plan A 기준)
1. (도메인 port) set 조회 메서드 추가/교체.
2. (ma-db-core) DAO `inList` 조회 + Repository 매핑 (+ DAO 테스트).
3. (도메인) `collector.collect(List)` 그룹핑 구현, `archiver.archive(List)`.
4. (배치) `MemberBackupItemWriter` 신설, JobConfig를 Composite Writer로, Processor 삭제.
5. 컴파일 + 회귀(`:ma-db-core:test`, `:boot:ma-boot-batch:test`, `:domain:ma-domain-core:test`).

## 트레이드오프 / 재검토
- **조기 최적화 주의**: 만료 집합이 소규모(수십~수백)면 240쿼리도 부담이 아닐 수 있음. **대량(수천+/run) 예상일 때만 가치.** 그렇지 않으면 보류 권장.
- **업로드 수는 그대로**(per-member 파일 유지). S3 요청수 절감은 별건(집계 파일, 조회성 희생).
- **메모리**: 청크 N명의 전체 연관 데이터를 메모리에 올림(이력 많은 회원 다수면 청크 N 축소 고려).
- **구조 회귀 가능성**: Plan B 선택 시 단건 Reader → 페이지 Reader 회귀.

## 미결정 (구현 전 확정)
1. **Plan A vs B** (Processor 유지 여부 vs 단건 Reader 유지 여부).
2. collector 전용 단건 `find(email)` 7종 — **제거** vs (혹시 모를 재사용 위해) 유지.
3. 청크 크기(20) 유지 vs 메모리 보고 조정.

## 참조
- 성능(N+1·벌크·exists): [[code-implementation-rules]] §18
- 포트·네이밍: [[code-implementation-rules]] §6·§13
- 모듈·패키지: [[clean-architecture]]
- 선행 백업 plan: `withdrawal-data-backup.plan.md`
