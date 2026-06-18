# Plan: 탈퇴 회원 데이터 백업-후-삭제 (전체 스냅샷 → Local, S3 후속)

- 작성일: 2026-06-13
- 작업 유형: 신규 기능 (탈퇴 정리 배치에 백업 단계 추가)
- 브랜치: `feat/withdrawal-anonymize-batch` (또는 후속 `feat/withdrawal-data-backup`)
- 선행: 탈퇴 정리 합성 리팩터 Phase 1·2 (같은 working tree, 미커밋) — `MemberWithdrawalCleaner.clean(member)` 존재 전제.

## ✅ 구현 완료 (2026-06-13, git 미커밋)

Local 어댑터까지 구현 완료. 전 모듈 컴파일·회귀 테스트 통과. **S3 어댑터만 후속 미구현.**
- 마스킹 확정: `password` 제외, `phoneNumber`→`PhoneNumber.masked()`(`010-****-1234`), name/email/birthDate 보존. `PhoneNumberTest`에 masked 테스트 추가.
- 조회 포트 보강: point-history/post/comment/post-like/comment-like/xroom에 `find(email)` + DAO/repo. support는 읽기 스택 신설(`Inquiry`/`InquiryEntity`/`InquiryQueryDao`/`InquiryQueryRepository`/`InquiryQueryCoreRepository`). `XroomEntity.from(row)` 신설.
- 백업 컴포넌트: `MemberWithdrawalBackup`(스냅샷)·`MemberBackupView`(마스킹뷰)·`MemberBackupStorage`/`MemberBackupSerializer`(포트, domain) + `MemberWithdrawalBackupCollector`·`MemberBackupArchiver`(domain @Component) + `LocalMemberBackupStorage`/`JacksonMemberBackupSerializer`(ma-file-storage).
- **도메인 컴포넌트 분리**: 정리·백업 로직을 도메인 @Component로 응집.
  - `MemberDataCleaner`(domain @Component) = 기존 batch `MemberWithdrawalCleaner`를 domain으로 이동·개명(13포트+8 cleanXxx). `clean(member)` = 삭제·익명화.
  - `MemberBackupArchiver`(domain @Component) = collector+serializer+storage. `archive(member)` = 수집→직렬화→저장.
- **Spring Batch R/P/W 분리(사용자 지시)**: Writer 하나에 Service로 몰아넣지 않고 표준 R/P/W로.
  - Reader `ExpiredWithdrawalMemberItemReader`(ItemReader<Member>) — 페이지 버퍼링 후 **단건 반환**(id 커서 ASC 유지).
  - Processor `MemberBackupItemProcessor`(Member→Member) — `archiver.archive(member)` 후 통과. **백업 단계**.
  - Writer `MemberPurgeItemWriter`(ItemWriter<Member>) — `dataCleaner.clean` 으로 청크 정리. **삭제·익명화 단계**.
  - "백업 후 삭제" 순서 = 청크 내 process→write 순서로 보장. 청크 크기 `CHUNK_SIZE_20`(트랜잭션·블래스트 반경 20명).
  - 단방 Service(`MemberWithdrawalCompletionService`)·`MemberCleanupItemWriter` **삭제** — 순서를 배치 파이프라인이 표현(배치 전용 플로우).
- 와이어링: JobConfig가 `archiver`·`cleaner`(도메인 컴포넌트) 주입 → P/W에 연결만. batch에 `ma-file-storage` runtimeOnly 추가(기존 photo FileStorage 갭도 동시 해소). 백업 키 `withdrawal-backup/{memberId}.json`. base-path 설정(local/test) 추가.
- 직렬화 안전성 확인: 백업 도메인 모델에 throw하는 getter 없음. `matchRate`(lazy)·`isVisible()`·`getRemainingDays()`는 안전한 계산 → JSON에 부가 필드로 포함될 뿐 예외 없음. `FAIL_ON_EMPTY_BEANS` 비활성으로 추가 안전망.
- **남은 결정/후속**: 미결정 6(name/email 보존으로 확정) / S3 어댑터(@Profile prod) + Local의 prod 제외(@Profile) / 백업 직렬화 통합 스모크 테스트 / 보존주기·암호화·접근통제 정책.

## 배경 / 문제

현재 `memberWithdrawalCompleteJob`은 유예 만료 회원을 **백업 없이** 바로 삭제·익명화한다. 복구 불가능한 **하드 삭제**(매칭 register행·타겟정보·포인트잔액·xroom·좋아요)가 포함되어, 분쟁·감사·오삭제 시 복원 수단이 없다.

→ `clean(member)` 실행 전에 **회원 전체 데이터 스냅샷을 떠서 저장소에 업로드**하고, 그 다음에 삭제·익명화한다. 저장소는 prod=AWS S3, local=로컬 VM 파일시스템.

## 결정된 요구사항 (사용자 확정)

1. **백업 범위 = 전체 회원 스냅샷.** Member + 삭제·익명화되는 연관 데이터(매칭·포인트·커뮤니티·문의·xroom·사진 메타) 전부.
   - **목적 = 소비자 분쟁 시 근거 자료.** 익명화하면 DB 원본이 사라지므로(글·댓글·문의·포인트이력도 익명화로 작성자 추적 불가) 백업이 유일한 근거. → 범위 축소(하드 삭제분만) 안 함.
2. **유출 위험 필드는 마스킹해서 저장.** 백업 파일이 새도 치명 정보가 노출되지 않도록 민감 식별자만 마스킹/제외(아래 "## 마스킹 규칙").
3. **이번 범위 = 포트 + 백업 로직 + Local 어댑터까지.** S3 어댑터(`@Profile("prod")`)는 버킷/리전/자격증명 확정 후 후속.

## 마스킹 규칙

> 백업 대상 도메인에 주민등록번호는 없음. 결제도 `PaymentMethod` enum뿐(카드/계좌 원본번호 없음)이고 정리 대상도 아님. 실제 민감 데이터는 아래.

| 필드 | 처리 | 방식 |
|---|---|---|
| `Member.password` (해시) | **제외** (백업 안 함) | 스냅샷에 미포함 |
| `Member.phoneNumber` | **마스킹** | `PhoneNumber.masked()` 도메인 행위 → `010-****-1234`(중간 4자리 마스킹). matching/auth 등 다른 도메인에 박힌 전화번호가 있으면 동일 적용 |
| `Member.name`(실명)·`email`·`birthDate` | **기본 보존**(근거자료) — 정책상 필요 시 부분 마스킹 가능 | 미결정 6 |

- 마스킹은 **스냅샷 조립 시점**에 도메인 행위로 수행(직렬화 시점 아님). VO가 스스로 마스킹 값을 제공(§2 도메인 행위 부여). 회원 식별은 `memberId`(+ `withdrawn_{id}` 이메일)로 가능하므로 전화번호 원본 없이도 분쟁 대응 가능.

## 핵심 비용: 도메인별 조회 포트 부재

전체 스냅샷은 정리(삭제/익명화) **전에** 모든 데이터를 읽어야 한다. Cleaner는 현재 Command(삭제/익명화) 포트만 들고 있어, 회원 단위 **조회 포트**를 도메인별로 보강해야 한다.

| 도메인 | 백업에 필요한 조회 | 현재 상태 | 신규 필요 |
|---|---|---|---|
| Member | `findOne(email)` 전체 PII | ✅ 있음 | - |
| TargetInfo | `find(email)` | ✅ 있음 | - |
| MatchingResult | `find(email)`(register) + `findClaimedByTarget(email)`(target) | ✅ 있음 | - |
| MemberPoint | `findOneOrInitial(ownerEmail)` 잔액 | ✅ 있음 | - |
| MemberPhoto | `findOne(email)` | ✅ 있음 | - |
| PointHistory | 소유자별 이력 전체 | ❌ `findOneOrNull(key)`만 | `find(ownerEmail): List<PointHistory>` |
| Post | 작성자별 글 | ❌ 없음 | `find(authorEmail): List<Post>` |
| Comment | 작성자별 댓글 | ❌ 없음 | `find(authorEmail): List<Comment>` |
| PostLike | 회원별 좋아요 | ❌ delete만 | `find(email): List<PostLike>` |
| CommentLike | 회원별 좋아요 | ❌ delete만 | `find(email): List<CommentLike>` |
| Inquiry | 작성자별 문의 | ❌ Query 포트 자체 부재 | 신규 `InquiryQueryRepository.find(authorEmail)` + DAO |
| Xroom | 회원별 xroom | ❌ `exists`만 | `find(email): List<Xroom>` |

각 신규 항목 = **포트 메서드 + DAO 메서드 + Entity `from(row)`/`toDomain()` + Repository 구현 (+ DAO 테스트)**. 이것이 본 작업 공수의 대부분이다.

> 메서드 네이밍은 [[code-implementation-rules]] §13 준수: 파라미터로 유추되는 조건은 메서드명에서 생략 → `find(email)`, `find(authorEmail)` (BAD: `findByMemberEmail`). 단건 아님 → `find`(복수).

## 목표 구조

```
ExpiredWithdrawalMemberItemReader → MemberCleanupItemWriter
                                       └→ MemberWithdrawalCleaner.clean(member)
                                            ├─ 1) backupService.backup(member)   ← 신규 (삭제 전)
                                            └─ 2) cleanAuth … anonymizeMember     ← 기존
```

### 신규 컴포넌트 / 포트

| 구성요소 | 책임 | 위치 |
|---|---|---|
| `MemberWithdrawalBackup` | 회원 스냅샷 데이터 캐리어(Member + 연관 도메인 컬렉션 보유). 일급 컬렉션/has-a 참조 | `domain/.../member/domain/` (신규 도메인 금지 — member 하위, [[feedback_no_new_domain]]) |
| `MemberWithdrawalBackupCollector` | 조회 포트들로 `MemberWithdrawalBackup` 조립 | `domain/.../member/application/` 또는 batch 모듈 |
| `BackupSerializer` (port) + Jackson 구현 | 스냅샷 → `ByteArray`(JSON). 포맷 의존을 인프라로 격리 | port: domain / 구현: 인프라(`ma-file-storage` 등) |
| `MemberBackupStorage` (port) | `store(directory, fileName, content: ByteArray)` | port: `domain/.../member/domain/port` 또는 `common` |
| `LocalMemberBackupStorage` | 로컬 VM 파일시스템 저장 (base-path 프로필 설정) | `infrastructure/support/ma-file-storage` |
| `S3MemberBackupStorage` | **후속** (`@Profile("prod")`, AWS SDK) | 동 모듈 |
| `MemberWithdrawalBackupService` | collector → serializer → storage 오케스트레이션(`backup(member)`) | `domain/.../member/application/` |

### `clean(member)` 변경
- 맨 앞에 `backupService.backup(member)` 추가 후 기존 8개 정리 호출. 순서: **백업 → 삭제/익명화**.
- Cleaner는 `MemberWithdrawalBackupService` 1개만 추가 의존(조회 포트들은 service/collector가 보유 — Cleaner 비대화 방지).

## 백업 동작 규칙

- **순서 = 업로드 먼저, 삭제 나중.** 업로드 실패 시 예외 → 그 회원 정리 중단 → 다음 실행 재시도(`deleted=false` 필터로 재진입, 데이터 유실 없음).
- **멱등성**: 백업 키 = memberId 기반(예: `withdrawal-backup/{yyyy-MM-dd}/{memberId}.json`) → 재시도 시 덮어쓰기. 삭제 트랜잭션이 롤백돼도 백업은 무해(다음 실행에서 갱신).
- **포맷**: 회원 1명당 JSON 파일 1개(전체 스냅샷).

## 기존 인프라 재사용 판단

- 이미 `FileStorage` 포트(`storeBytes(directory, fileName, bytes)`) + `LocalFileStorage`가 존재.
- **D-옵션(결정 필요)**: 백업 저장을 (a) 기존 `FileStorage` 재사용 vs (b) 전용 `MemberBackupStorage` 신설.
  - 권장 = (b) 전용 포트. 백업은 사용자 사진 저장과 **버킷·보존주기·라이프사이클이 다름**(S3 시 별 버킷/Glacier 등). 로컬 어댑터는 `LocalFileStorage` 로직과 유사하나 base-path만 다르게.
  - (a) 재사용은 클래스 0개 추가로 가장 싸지만 사진/백업 관심사가 한 어댑터에 섞임.

## 미결정 (구현 전 확정)

1. **백업 저장 포트**: 전용 `MemberBackupStorage`(권장) vs `FileStorage` 재사용. (위 D-옵션)
2. **직렬화 위치**: `BackupSerializer` 포트로 분리(권장, 도메인 Jackson 비의존) vs 저장 어댑터가 직접 직렬화.
3. **백업과 트랜잭션 경계**: 현재 단일 step 청크 트랜잭션 **안에서** 백업(읽기+업로드) 수행(간단, local FS는 빠름) vs 별도 선행 처리로 분리(S3 네트워크 지연이 DB 트랜잭션 점유하는 문제 회피). → 권장: 이번엔 `clean()` 선두에서 수행, S3 어댑터 도입 시 재검토.
4. **백업 키/디렉토리 규칙·보존주기**: `withdrawal-backup/{date}/{memberId}.json` 제안. 보존주기(라이프사이클)는 S3 후속에서.
5. **PointHistory 규모**: 이력이 매우 많은 회원은 한 JSON이 커질 수 있음 — 페이징 수집/스트리밍 필요 여부.
6. **name·email·birthDate 마스킹 여부**: 기본 보존(근거자료) vs 부분 마스킹. 백업 저장소 접근통제·암호화가 충분하면 보존, 정책상 추가 보호가 필요하면 부분 마스킹.

## 영향 범위 (파일)

### 신규
| 파일 | 내용 |
|---|---|
| `domain/.../member/domain/MemberWithdrawalBackup.kt` | 스냅샷 데이터 캐리어 (마스킹된 값 보유, `password` 미포함) |
| `domain/.../member/domain/PhoneNumber.kt` (수정) | `fun masked(): String` 추가 → `010-****-{lastNumber}` |
| `domain/.../member/application/MemberWithdrawalBackupCollector.kt` | 조회 포트 조립 |
| `domain/.../member/application/MemberWithdrawalBackupService.kt` | `backup(member)` |
| `domain/.../member/domain/port/MemberBackupStorage.kt` | 저장 포트 |
| `domain/.../common/.../port/BackupSerializer.kt` | 직렬화 포트 |
| `infrastructure/support/ma-file-storage/.../LocalMemberBackupStorage.kt` | 로컬 어댑터 |
| `infrastructure/support/ma-file-storage/.../JacksonBackupSerializer.kt` | 직렬화 구현 |
| (후속) `S3MemberBackupStorage.kt` + AWS SDK 의존성 | prod 어댑터 |

### 수정 (조회 포트 보강 — 도메인 port + ma-db-core DAO/Repository)
| 도메인 | port | dao/repository |
|---|---|---|
| point | `PointHistoryRepository.find(ownerEmail)` | Command/Query DAO + repo + entity 매핑 |
| community | `PostQueryRepository.find(authorEmail)`, `CommentQueryRepository.find(authorEmail)`, `PostLikeRepository.find(email)`, `CommentLikeRepository.find(email)` | 동 |
| support | `InquiryQueryRepository`(신규) `find(authorEmail)` | 신규 DAO/repo |
| xroom | `XroomQueryRepository.find(email)` | 동 |

### 수정 (배치)
| 파일 | 변경 |
|---|---|
| `MemberWithdrawalCleaner.kt` | `clean()` 선두에 `backupService.backup(member)` + 의존성 1개 추가 |

## 구현 순서

1. (도메인 port) 백업에 필요한 조회 포트 메서드 보강 — point/community/support/xroom.
2. (ma-db-core) 해당 DAO/Repository/Entity 매핑 구현 + DAO 테스트([[feedback_impl_then_test]]).
3. (도메인) `PhoneNumber.masked()` 행위 + `MemberWithdrawalBackup` 데이터 캐리어(마스킹·password 제외) + `MemberBackupStorage`/`BackupSerializer` 포트.
4. (인프라) `LocalMemberBackupStorage` + `JacksonBackupSerializer` + 프로필 설정(`ma-file-storage/config/application-local.yml`).
5. (도메인 application) `MemberWithdrawalBackupCollector` + `MemberWithdrawalBackupService`.
6. (배치) `Cleaner.clean()` 선두에 백업 호출 와이어링.
7. 컴파일 + 회귀(`:boot:ma-boot-batch:test`, `:ma-db-core:test`) + 백업 동작 테스트.
8. (후속 별건) `S3MemberBackupStorage` + AWS SDK + prod 프로필 — 버킷/리전/자격증명 확정 시.

## 리스크 / 주의

- **공수 大**: 조회 포트 6종 신설(특히 Inquiry는 Query 포트 자체 신설)이 본체. 전체 스냅샷 결정의 직접 비용.
- **N+1 / 대용량**: 회원 1명당 도메인 6~8회 조회. 페이지 20 × 도메인 = 트랜잭션·메모리 부담. 이력 많은 회원 JSON 비대화(미결정 5).
- **트랜잭션 점유**: S3 도입 시 업로드 지연이 청크 트랜잭션을 잡음(미결정 3).
- **개인정보 파기 vs 백업 보존**: 백업은 분쟁 근거자료로 **필요**하므로 보존하되, 유출 리스크는 (1) 민감 식별자 마스킹/제외(위 규칙), (2) 저장소 접근통제, (3) 저장 암호화, (4) 보존주기 후 자동 만료(S3 lifecycle)로 완화한다. 보존기간 법적 근거는 후속 정책 확인 권장.
- **삭제는 git rm / 파일 이동은 git mv** ([[feedback_git_mv]]).

## 구현 시 참조

- 합성·도메인 행위·포트 규칙: [[code-implementation-rules]] (§6 포트, §13 네이밍, §18 성능)
- 모듈·패키지 배치: [[clean-architecture]]
- 네이밍/가독성: [[clean-code]]
- 테스트(KoTest/Mockk): [[kotest-writing]]
- 삭제 vs 익명화 기준(백업 대상 식별): [[member-withdrawal-cleanup]]
- 합성 리팩터(선행, clean() 구조): `withdrawal-cleanup-composition-refactor.plan.md`
