# Plan: GET /api/matching-results/claimed-by 재설계 (Claimer 중심 응답)

> 작성일: 2026-04-17

## 1. 개요

"나를 X로 신청한 사람 목록" API를 **MatchingResult 중심** 응답에서 **Claimer(요청자) 중심** 응답으로 재설계한다.
요청자 프로필(id/name/nickname/profileImageUrl)과 "해당 요청자가 나를 대상으로 한 MatchingResult의 targetInfo로 이미 X룸을 만들었는지"를 함께 제공하여, 프론트가 리소스를 오해 없이 다룰 수 있도록 한다.

### 현재 구조의 문제점
- 응답이 `MatchingResultResponse`를 재사용하므로 `matchRate/remainingDays/claimed` 같이 **요청자 관점 정보**가 타겟(로그인 사용자)에게 노출된다.
- `MatchingResultWithProfile.targetMemberId/targetName` 필드가 "타겟"과 "요청자"를 둘 다 의미하여 의미가 혼선된다.
- 프론트가 이 API 응답을 "내 매칭 결과"와 같은 리소스로 오해할 수 있다.

### `hasXroom` 플래그의 의미 정의 (중요)
"로그인 사용자 기준으로 X룸이 있는가"가 아니라, **요청자(Claimer)가 `MatchingResult.targetInfoId`(= 요청자가 등록한 targetInfo, 그 결과로 나를 찾은 정보)로 X룸을 이미 만들었는가** 를 나타낸다.
- `XroomTable.targetInfoId`는 "X룸을 만든 사람의 targetInfoId"이며,
- `MatchingResult.targetInfoId`는 "해당 MatchingResult를 만든 사람(= `registerEmail` = Claimer)의 targetInfoId"이다.
- 따라서 `XroomTable`에 `matchingResult.targetInfoId`가 존재하면 → "이 요청자가 나를 대상으로 이미 X룸을 만들었다" = `hasXroom = true`.
- N개 MatchingResult의 targetInfoId 집합에 대해 **한 번의 IN 쿼리**로 존재 여부를 조회한다(N+1 금지).

---

## 2. 변경 전략

### 2.1 레이어별 변환 규칙

| 레이어 | 현재 | 변경 후 | 변환 위치 |
|--------|------|---------|-----------|
| Controller 반환 | `MatchingResultsResponse` | `ClaimersResponse` | `MatchingResultQueryApi.findClaimedByMe` |
| Service 반환 | `MatchingResultsWithProfiles` | `ClaimerProfiles` | `MatchingResultQueryService.findClaimedBy` |
| 도메인 조합 | `MatchingResults.combineWithClaimerProfiles(members, photos)` | `MatchingResults.toClaimerProfiles(members, photos, xroomExistTargetInfoIds)` | `MatchingResults.kt` |
| 조합 결과 타입 | `MatchingResultsWithProfiles`(MatchingResult 중심) | `ClaimerProfiles`(요청자 중심) | 신규 도메인 모델 |
| Xroom 존재 조회 | `existsByTargetInfoId(id: Long): Boolean` (단건) | 단건 유지 + `existsByTargetInfoIds(ids: List<Long>): Set<Long>` (벌크) 추가 | 포트 오버로드 |

### 2.2 신규 도메인 모델

**위치**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/`
> 새 도메인을 만들지 않고 matching 하위 패키지에 둔다. (메모리의 `feedback_no_new_domain`)

- `ClaimerProfile`
  - 필드: `memberId: Long?`, `name: String?`, `nickname: String?`, `profileImageUrl: String?`, `hasXroom: Boolean`
  - 파생 필드: `isWithdrawn: Boolean`(= `memberId == null`)
  - nullable 처리 사유: `claimed-by` 목록에는 탈퇴 회원의 레코드가 섞일 수 있으므로 기존 `MatchingResultWithProfile`과 동일하게 nullable 허용
- `ClaimerProfiles(val data: List<ClaimerProfile>)` — 일급 컬렉션 (컨벤션: 멤버 변수 `val data`)

### 2.3 MatchingResults 변환 메서드 교체

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 메서드명 | `combineWithClaimerProfiles(members, photos): MatchingResultsWithProfiles` | `toClaimerProfiles(members, photos, xroomExistTargetInfoIds: Set<Long>): ClaimerProfiles` |
| 조회 키 | `registerEmail` (동일) | `registerEmail` (동일) |
| `hasXroom` 계산 | 없음 | `xroomExistTargetInfoIds.contains(result.targetInfoId)` |
| 반환 요소 | `MatchingResultWithProfile`(MatchingResult 포함) | `ClaimerProfile`(요청자 정보만) |

`combineWithClaimerProfiles`는 외부 사용처가 `MatchingResultQueryService.findClaimedBy` 단 한 곳이므로 **제거**한다.
`combineWithProfiles`(타겟 프로필 조합)은 `/api/matching-results`에서 여전히 사용하므로 **유지**한다.

### 2.4 XroomQueryRepository 포트 변경

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 메서드 | `existsByTargetInfoId(targetInfoId: Long): Boolean` | 유지 |
| 메서드 추가 | - | `existsByTargetInfoIds(targetInfoIds: List<Long>): Set<Long>` |
| 반환 의미 | - | "존재하는 targetInfoId들의 집합"(존재하지 않는 id는 포함되지 않음) |
| 네이밍 근거 | - | 기존 메서드와 일관된 접두/접미 규칙(`existsByTargetInfoId*`) 유지. 프로젝트에 오버로드된 port 메서드 선례는 없으므로 **파라미터 타입 차이를 이름(`...Ids` 복수형)으로 구분**하여 호출부 가독성 확보 |
| 빈 리스트 처리 | - | 빈 리스트 입력 시 DB 쿼리 없이 `emptySet()` 반환 (DAO에서 가드) |
| 쿼리 형태 | 단건 limit 1 | `XroomTable.activeRows { targetInfoId inList ids }` → `select(targetInfoId)` 후 Set으로 수집 |

### 2.5 Service 변경 (findClaimedBy)

구현 순서 (결정적 — 변경 금지):
1. `matchingResults = MatchingResults(repository.findClaimedByTarget(Email(email)))`
2. `registerEmails = matchingResults.extractRegisterEmails()`
3. `members = Members(memberQueryRepository.findByEmails(registerEmails))`
4. `photos = MemberPhotos(memberPhotoRepository.find(registerEmails))`
5. `targetInfoIds = matchingResults.extractTargetInfoIds()` — 신규 추출 메서드 (아래 2.6 참조)
6. `xroomExistTargetInfoIds = xroomQueryRepository.existsByTargetInfoIds(targetInfoIds.toList())`
7. `return matchingResults.toClaimerProfiles(members, photos, xroomExistTargetInfoIds)`

크로스 도메인 의존 판정:
- 이미 `MatchingResultQueryService`가 `MemberQueryRepository`, `MemberPhotoRepository`를 의존하고 있고, 프로젝트 전반에서 Service → 타 도메인 Port 의존이 일반적인 패턴이다(`TargetInfoCommandService` 등).
- 따라서 `MatchingResultQueryService → XroomQueryRepository` 의존은 기존 컨벤션을 따르는 자연스러운 방식이다.

### 2.6 MatchingResults 부가 메서드

| 메서드 | 현재 | 변경 후 |
|--------|------|---------|
| `extractTargetInfoIds(): Set<Long>` | 없음 | 신규 추가 — `data.map { it.targetInfoId }.toSet()` |

### 2.7 응답 DTO

**위치**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/`

- `ClaimerResponse`
  - `memberId: Long?` (`@EncryptId(ObfuscationType.MEMBER)`)
  - `name: String?`
  - `nickname: String?`
  - `profileImageUrl: String?`
  - `isWithdrawn: Boolean`
  - `hasXroom: Boolean`
  - `from(profile: ClaimerProfile): ClaimerResponse` 팩토리
- `ClaimersResponse(val claimers: List<ClaimerResponse>)`
  - `from(profiles: ClaimerProfiles): ClaimersResponse` 팩토리

### 2.8 URL 결정

| 옵션 | 장점 | 단점 |
|------|------|------|
| `/api/matching-results/claimed-by` (유지) | 프론트/문서 변경 없음, 본 리소스가 "matching-result에서 파생된 요청자 목록"이라는 의미를 보존 | 응답 리소스와 URL 경로 용어가 살짝 어긋남 |
| `/api/claimers` 또는 `/api/matching-results/claimers` | 응답 리소스(`ClaimersResponse`)와 URL 용어 일치 | 기존 프론트/문서 수정 필요, 리소스 계층 재정의 비용 |

**결정: 유지 (`/api/matching-results/claimed-by`)**
- 이 API는 "내 MatchingResult 중 claim된 건의 요청자 목록"이라는 **서브 리소스** 관점으로 해석 가능하며, 현재 URL이 해당 의미를 잘 표현한다.
- URL 이동은 클라이언트 연동 비용이 크고, 본 변경의 핵심 가치(응답 구조 재설계)와 분리되어 있다.

---

## 3. 변경 파일 목록

### Phase 1: Xroom 포트/어댑터 (벌크 exists 추가)
| # | 파일 | 내용 |
|---|------|------|
| 1 | `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/xroom/domain/port/XroomQueryRepository.kt` | `existsByTargetInfoIds(ids: List<Long>): Set<Long>` 메서드 추가 |
| 2 | `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/dao/XroomQueryDao.kt` | 벌크 exists 구현 (`inList` + `select(targetInfoId)` → `Set<Long>`, 빈 리스트 가드) |
| 3 | `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/repository/XroomQueryCoreRepository.kt` | 벌크 exists 위임 오버라이드 |

### Phase 2: Matching 도메인 모델
| # | 파일 | 내용 |
|---|------|------|
| 4 | `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/ClaimerProfile.kt` | 신규 — 필드 5개 + `isWithdrawn` 파생 속성 |
| 5 | `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/ClaimerProfiles.kt` | 신규 — 일급 컬렉션(`val data: List<ClaimerProfile>`) |
| 6 | `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResults.kt` | `combineWithClaimerProfiles` 제거, `toClaimerProfiles(members, photos, xroomExistTargetInfoIds)` 추가, `extractTargetInfoIds()` 추가 |

### Phase 3: Matching 애플리케이션 Service
| # | 파일 | 내용 |
|---|------|------|
| 7 | `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryService.kt` | `XroomQueryRepository` 주입, `findClaimedBy` 시그니처/구현 교체(`ClaimerProfiles` 반환) |

### Phase 4: Web API
| # | 파일 | 내용 |
|---|------|------|
| 8 | `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/ClaimerResponse.kt` | 신규 — Claimer 단건 응답 DTO, `from(ClaimerProfile)` 팩토리 |
| 9 | `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/response/ClaimersResponse.kt` | 신규 — Claimer 목록 응답 DTO, `from(ClaimerProfiles)` 팩토리 |
| 10 | `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApi.kt` | `findClaimedByMe` 반환 타입을 `ClaimersResponse`로 교체 |

### Phase 5: 테스트
| # | 파일 | 내용 |
|---|------|------|
| 11 | `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/matching/domain/MatchingResultsTest.kt` | `combineWithClaimerProfiles` 관련 테스트 제거 → `toClaimerProfiles` 테스트 추가 (hasXroom true/false, 탈퇴 회원, 빈 집합, 여러 건 혼재 시나리오) |
| 12 | `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/matching/application/MatchingResultQueryServiceTest.kt` | `findClaimedBy` 테스트 — `XroomQueryRepository` mock 추가, `ClaimerProfiles` 반환 검증 |
| 13 | `infrastructure/storage/ma-db-core/src/test/kotlin/com/konkuk/ma/domain/xroom/dao/XroomDaoTest.kt` | `existsByTargetInfoIds` 테스트 추가 (존재하는/없는 ID 혼합, 빈 리스트, soft-deleted 제외 검증) |
| 14 | `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/domain/matching/api/MatchingResultQueryApiTest.kt` | `findClaimedByMe` 시나리오 교체 — `ClaimerProfiles` 스텁, `ClaimersResponse` 문서화, 응답 필드 재구성 |
| 15 | `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/vocabulary/MatchingVocabulary.kt` | Claimer 응답 필드 Vocabulary 추가(`claimerMemberId`, `claimerName`, `claimerNickname`, `claimerProfileImageUrl`, `claimerIsWithdrawn`, `hasXroom`) — 경로 prefix `claimers[].*` |

### Phase 6: REST Docs AsciiDoc
| # | 파일 | 내용 |
|---|------|------|
| 16 | `boot/ma-boot-web/src/docs/asciidoc/matching/find-claimed-by.adoc` | 기존 문서가 있다면 Claimer 중심 스니펫으로 갱신, 없다면 신규 작성 |
| 17 | `boot/ma-boot-web/src/docs/asciidoc/main.adoc` | `find-claimed-by.adoc` include 확인/갱신 |

### Phase 7: 문서
| # | 파일 | 내용 |
|---|------|------|
| 18 | `docs/api-todo.md` | 해당 API 재설계 항목 완료 표시 (도메인 섹션 유지, `feedback_api_todo_keep_domain` 준수) |

---

## 4. 고려사항

### 4.1 성능 / N+1 방지
- `existsByTargetInfoIds`는 **단일 IN 쿼리**로 N개 MatchingResult의 targetInfoId 존재 여부를 한번에 조회한다.
- 빈 리스트가 입력되면 DB를 치지 않고 `emptySet()`을 반환하여 불필요한 쿼리를 제거한다.
- MatchingResults가 매우 크더라도 targetInfoId 집합은 MatchingResult 수와 동일 상한이므로 IN 절 크기는 실용 범위 내.

### 4.2 DB 인덱스
- `XroomTable.targetInfoId`는 `xroom-create`에서 **UNIQUE INDEX**로 생성됨. 따라서 `IN` 질의는 인덱스 레인지 스캔으로 최적화된다.
- DDL 변경 없음.

### 4.3 FK 사용 안 함
- 메모리 `feedback_no_fk`에 따라 FK는 사용하지 않으며, 본 변경도 DDL을 건드리지 않는다.

### 4.4 크로스 도메인 의존 허용 근거
- `domain/.../application/*Service`가 타 도메인의 **Port 인터페이스**를 의존하는 것은 이 프로젝트의 관행(예: `MatchingResultQueryService`의 `MemberQueryRepository` 의존, `TargetInfoCommandService`의 `MemberQueryRepository` 의존).
- Domain Model(예: `MatchingResults`)은 외부 도메인 포트를 의존하지 않고 **원시 집합(Set<Long>)을 주입받아 계산**한다 → 도메인 간 결합도 최소화.

### 4.5 기존 `MatchingResultWithProfile` / `MatchingResultsWithProfiles`
- `/api/matching-results`(본인 매칭 결과 목록)에서 여전히 사용하므로 **그대로 유지**한다.
- `findClaimedBy`만 새 모델(`ClaimerProfile/ClaimerProfiles`)로 이동한다.

### 4.6 도메인 객체 위치
- `ClaimerProfile`, `ClaimerProfiles`는 matching 바운디드 컨텍스트의 "조회 결과 관점" 모델이므로 `matching/domain/` 하위에 배치한다.
- 별도 패키지(예: `matching/domain/claimer/`)를 만들지 않는다 — 현재 유사 모델(`MatchingResultWithProfile`)도 `matching/domain/` 바로 아래에 있어 컨벤션 일관.

### 4.7 응답 경로 / 네이밍
- 응답 루트 배열 필드명은 `claimers`로 한다 (`matchingResults`와 명확히 구분되는 의미).
- Vocabulary 경로 prefix도 `claimers[].*`로 통일하여 REST Docs 스니펫이 응답 구조를 그대로 반영하도록 한다.

### 4.8 하위 호환성
- **Breaking change**: 프론트 응답 스키마가 바뀐다(`matchingResults[...]` → `claimers[...]`, 필드 구성 변경).
- URL은 유지되므로 엔드포인트 자체는 프론트 코드에서 그대로 호출 가능하나, 응답 파싱 로직은 교체가 필요하다.
- 롤아웃 전 프론트와 동기화 필요.

### 4.9 URL 유지 vs 변경
- 4.8의 Breaking change를 감수하는 대신, URL은 유지하여 라우팅 테이블/문서 링크 변경을 피한다(결정 근거: 2.8).

---

## 5. 검증 항목

### 5.1 도메인 테스트 (`MatchingResultsTest`)
- [ ] `toClaimerProfiles` — members/photos가 모두 매핑되는 정상 케이스, 사진 없음, 탈퇴 회원(member/photo 없음) 혼재
- [ ] `toClaimerProfiles` — `xroomExistTargetInfoIds`가 빈 집합이면 모든 `hasXroom = false`
- [ ] `toClaimerProfiles` — `xroomExistTargetInfoIds`에 일부 targetInfoId만 포함되면 해당 건만 `hasXroom = true`
- [ ] `extractTargetInfoIds` — 중복 제거 확인

### 5.2 DAO 테스트 (`XroomDaoTest`)
- [ ] `existsByTargetInfoIds` — 존재하는 ID 일부 + 없는 ID 일부 혼합 시 존재하는 것만 반환
- [ ] `existsByTargetInfoIds` — 빈 리스트 입력 시 DB 쿼리 없이 `emptySet()` 반환 (쿼리 미발생 검증은 어려우면 결과만 검증)
- [ ] `existsByTargetInfoIds` — soft-deleted 레코드는 결과에서 제외 (`activeRows` 필터)

### 5.3 Service 테스트 (`MatchingResultQueryServiceTest`)
- [ ] `findClaimedBy` — `MatchingResultRepository.findClaimedByTarget`, `MemberQueryRepository.findByEmails`, `MemberPhotoRepository.find`, `XroomQueryRepository.existsByTargetInfoIds`가 모두 호출되고 결과가 `ClaimerProfiles`로 반환됨

### 5.4 API 테스트 (`MatchingResultQueryApiTest`)
- [ ] `findClaimedByMe` 문서화 — 응답 구조가 `claimers[].*`로 재구성됨
- [ ] `hasXroom`이 true/false 각각 포함된 예시 문서화 가능 (시나리오 2개 or 한 문서 내 혼합)
- [ ] `@BaseApiTest`에 포함된 `@WithAuthMember` 중복 선언 없음 (`feedback_no_duplicate_with_auth_member`)

### 5.5 빌드 / 문서
- [ ] `./gradlew build` 성공
- [ ] `boot/ma-boot-web/src/main/resources/static/docs/index.html`에 `find-claimed-by` 섹션이 새 응답 스키마로 갱신됨
- [ ] `docs/api-todo.md`에 본 항목 완료 처리 (도메인 헤더 유지)

---

## 6. 구현 순서 요약

1. **Phase 1** — Xroom 포트/DAO/Repository 벌크 exists 추가 + DAO 테스트 작성 → 그린 확인
2. **Phase 2** — `ClaimerProfile`, `ClaimerProfiles`, `MatchingResults.toClaimerProfiles`/`extractTargetInfoIds` 작성 + 도메인 테스트 그린 확인
3. **Phase 3** — `MatchingResultQueryService.findClaimedBy` 교체 + Service 테스트 그린 확인
4. **Phase 4** — `ClaimerResponse`, `ClaimersResponse`, `MatchingResultQueryApi` 반환 타입 교체
5. **Phase 5~6** — API 테스트/Vocabulary/REST Docs asciidoc 갱신
6. **Phase 7** — `api-todo.md` 업데이트
7. **최종** — `./gradlew build` 전체 그린 확인
