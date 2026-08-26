# Plan: REQ-016 — 내 프로필 조회·수정 (`GET/PATCH /api/members/me`)

- 작성일: 2026-08-20
- 작업 유형: 기능 개발 (프론트 요청 대응) + 기존 저장 규약 정리
- 대상 저장소: meet-again-backend (base 브랜치 `develop`, 기준 커밋 `a399e60`)
- 스펙 단일 소스: 프론트 `origin/develop:docs/to-backend.md` 의 **REQ-016** (83f4814, 2026-08-15)

---

## 1. 배경

프론트 `lib/screens/etc/profile_edit_screen.dart` (화면 제목 **"내 정보 관리"**)가 하드코딩 더미 데이터를 걷어내면서 표시할 값을 잃었다. 현재 상태:

| 화면 요소 | 지금 | 원인 |
|---|---|---|
| 닉네임 · 이메일 | read-only 표시 | `AuthService` 로컬 저장값 (로그인 응답에 담겨 옴) |
| 이름 · 성별 · 학교 · 거주지 | 필드 자체가 없음 | 내려줄 API 부재 |
| 프로필 사진 | `Icons.person` 플레이스홀더 | 사진 URL 을 주는 API 부재 |
| 수정 버튼 | "수정 기능 준비 중" 스낵바 | 수정 API 부재 |

### 확인된 사실 (코드 대조)

| 확인 항목 | 결과 | 근거 |
|---|---|---|
| `GET /api/members/me` 존재 | 없음 | `MemberQueryApi` = 중복확인 2개뿐 |
| `PATCH /api/members/me` 존재 | 없음 | 회원 도메인에 수정 엔드포인트 0건 |
| 조회 경로 | **이미 있음** | `MemberQueryService.findOne(id)` |
| 수정 경로 | 없음 | `MemberCommandRepository` = save · updatePassword · requestWithdrawal · cancelWithdrawal · anonymizeAndSoftDelete |
| `Member` 수정 행위 | 없음 | 전 필드 `val`, `anonymize()` 는 새 인스턴스 반환 |
| 응답 필드 보유 | `profileImageUrl` 만 없음 | `Member` 에 나머지 10개 전부 존재 |
| 프론트가 memberId 를 아는가 | **모른다** | `LoginResponse` = email · nickname · accessToken · refreshToken |
| `MemberApi.getMember()` | 사문 | `GET /api/members/{memberId}` 호출, 백엔드에 없고 호출처 0건 |
| `MEMBERS.PROFILE_IMAGE_URL` | 죽은 컬럼 | 읽는 코드 0건, `MemberCommandDao` 익명화 시 null 대입뿐 |

### 이 작업이 건드리게 되는 두 번째 문제

`profileImageUrl` 에는 선례가 둘 있고 **구형이 깨져 있다.**

| 패턴 | 사용처 | 저장·반환 값 | 상태 |
|---|---|---|---|
| 구형 | `MatchingResultResponse` · `ClaimerResponse` | `photo.thumbnailPath` 원시값 = `LocalFileStorage.store()` 반환 = `uploads/member/thumbnail/1/thumb_x.jpg` | **URL 이 아니라 서버 파일시스템 경로.** 클라이언트가 못 쓰고 경로가 노출된다 |
| 신형 | `MediaProcessor`+`MediaUrlResolver` (xroom) · `PostImageProcessor`+`PostImageUrlResolver` (community) | 상대 `storageKey` 저장 → `FileUrlResolver.resolve()` → Local `/files/{key}` · S3 presigned | 현재 표준 |

---

## 2. 목표

1. `GET /api/members/me` — 로그인한 회원의 프로필 전체를 내려준다.
2. `PATCH /api/members/me` — 프로필을 부분 수정한다.
3. `MemberPhoto` 저장 규약을 신형(상대 storageKey + `FileUrlResolver`)으로 전환하고, 그에 딸린 매칭·claimer 응답의 깨진 `profileImageUrl` 까지 정리한다.
4. 프로필 사진이 프론트에서 헤더 없이 표시되도록 `/files/member/**` 를 공개한다.
5. 닉네임 중복을 DB 유니크 인덱스로 백스톱한다.

### 범위 밖

- `MEMBERS.PROFILE_IMAGE_URL` 죽은 컬럼 제거 — 별건 (api-todo 기록만)
- 프로필 사진 `APPROVAL_STATUS` 승인 워크플로 — 현재 어디서도 검사하지 않으며, 이번에도 검사하지 않는다 (기존 매칭 응답과 동일 동작 유지)
- 이메일 · 생년월일 · 전화번호 수정 — 프론트가 읽기 전용으로 명시

---

## 3. 확정된 설계 결정 (2026-08-20 사용자 승인)

### D1. `name` · `gender` 는 수정 대상에서 제외한다 — **채택: A안**

두 값은 **매칭의 하드 필터**다. 매칭 배치가 회원 행을 실시간으로 읽어 검색 대상을 만든다:

```
Member(name·gender·region·phoneNumber·birthDate)
  → Target.create(member)                       [Targets.from]
  → Targets.filterCandidates(name, gender)      ← name·gender 로 후보를 거른다
  → TargetInfo.makeMatchingResults              ← region 은 regionMatched 플래그일 뿐
```

- `name` 변경 = 나를 찾던 사람이 나를 못 찾게 된다.
- `gender` 변경 = 위와 같고, 추가로 `TargetInfoCommandService.register` 가 `member.getOtherGender()` 를 **TARGET_INFO 행에 저장**해 두므로 내가 등록한 TargetInfo 들의 `targetGender` 가 낡은 값으로 남는다.

⇒ PATCH 대상은 **`nickname` · `region` · `highSchool` · `university` 4필드**. 요청서의 6필드에서 2개가 빠지므로 **`to-backend.md` Backend Reply 에 이유를 적어 회신한다**(§11).

`region` 은 허용한다 — `regionMatched` 불리언만 바뀌고 후보를 거르지 않는다. 이미 생성된 `MatchingResult` 는 재계산하지 않는다(기존 동작과 동일).

### D2. 사진 저장 규약을 신형으로 전환하고 매칭 응답까지 정리한다 — **채택: B안**

`MemberPhotoProcessor` 를 `MediaProcessor` · `PostImageProcessor` 와 같은 형태로 맞춘다. 컬럼명도 신형에 맞춘다(`FILE_PATH`→`STORAGE_KEY`, `THUMBNAIL_PATH`→`THUMBNAIL_KEY`) — community `COMMUNITY_POST_IMAGES` 가 이미 `STORAGE_KEY`/`THUMBNAIL_KEY` 이므로 이름이 값과 어긋나지 않게 한다. 기존 행은 마이그레이션으로 접두사를 벗긴다(§9).

### D3. PATCH 는 부분 수정이다 — **채택: A안**

- 요청 본문에 **없는 필드**는 건드리지 않는다.
- **명시적 `null`** 은 "비우기"다. `highSchool` · `university` 만 해당(둘 다 nullable).
- `nickname` · `region` 은 비울 수 없다 — 생략(=변경 없음)이거나 유효한 값이거나 둘 중 하나.

**D3-a. 생략과 `null` 의 구분 방법**: 요청 DTO 의 nullable 두 필드를 `Optional<String>?` 로 선언한다. Jackson `Jdk8Module` (spring-boot-starter-json 에 포함, 자동 등록)이 *생략*→`null`, *명시적 null*→`Optional.empty()`, *값*→`Optional.of(v)` 로 매핑한다. `Optional` 은 api 계층 밖으로 새지 않게 하고, 도메인에는 tri-state 를 표현하는 값 객체 `Changed<T>` 로 넘긴다(§5).

### D4. `/files/member/**` 를 permitAll 한다

`SecurityConfig` 에 이미 `/files/memory/**` · `/files/community/**` 가 permitAll 이다. member 만 인증을 요구하면 Flutter `Image.network` 가 Authorization 헤더를 붙이지 않아 사진이 뜨지 않는다. 동일하게 공개한다.

### D5. `MEMBERS.NICKNAME` 에 유니크 인덱스를 건다

현재 `existsByNickname` 앱 레벨 검사만 있어 동시 요청에 뚫린다. 가입 때부터 있던 구멍이지만 수정 API 가 생기면 노출면이 넓어진다. **적용 전 기존 중복 정리가 선행**되어야 한다(§9).

---

## 4. API 계약

### `GET /api/members/me`

- Auth: Bearer 필수
- 200 응답 필드 11개: `memberId`(인코딩) · `email` · `nickname` · `name` · `gender` · `birthDate` · `phoneNumber` · `region` · `highSchool` · `university` · `profileImageUrl`
- `highSchool` · `university` · `profileImageUrl` 은 nullable
- `gender` = `MALE` | `FEMALE`, `region` = `Region` enum 코드(`SEOUL` 등, 선택지는 기존 `GET /api/members/regions`)

### `PATCH /api/members/me`

- Auth: Bearer 필수
- 요청 필드 4개, **전부 선택적**: `nickname` · `region` · `highSchool` · `university`
- 200 응답 = `GET /api/members/me` 와 동일한 11필드 (수정 후 값)

| 필드 | 생략 | `null` | 값 |
|---|---|---|---|
| `nickname` | 변경 없음 | 400 | 형식 검증 + 중복 검사 후 변경 |
| `region` | 변경 없음 | 400 | enum 검증 후 변경 |
| `highSchool` | 변경 없음 | 비우기 | 변경 |
| `university` | 변경 없음 | 비우기 | 변경 |

검증 규칙은 회원가입과 동일한 상수를 재사용한다 — `ValidationPatterns.NICKNAME` = `^[a-zA-Z가-힣0-9]{2,8}$`.

---

## 5. 변경 파일

### Part A — 프로필 조회·수정

#### 신규

| 파일 | 내용 |
|---|---|
| `boot/ma-boot-web/.../member/api/MemberProfileApi.kt` | `@RequestMapping("/api/members/me")`. `findMyProfile(@LoginMember)` (GET), `updateMyProfile(@LoginMember, @Validated @RequestBody)` (PATCH) |
| `boot/ma-boot-web/.../member/api/request/UpdateMyProfileRequest.kt` | `nickname: String?`, `region: Region?`, `highSchool: Optional<String>?`, `university: Optional<String>?` + `toProfileChanges(): ProfileChanges`. Bean Validation 은 nickname 패턴만 |
| `boot/ma-boot-web/.../member/api/response/MyProfileResponse.kt` | 11필드. `memberId` 에 `@EncryptId(ObfuscationType.MEMBER)`. `from(profile: MemberProfile)` 팩토리 |
| `domain/ma-domain-core/.../common/domain/Changed.kt` | tri-state 값 객체. `Changed<T>(val value: T?)` — 래퍼 자체가 `null` 이면 "변경 없음", `Changed(null)` 이면 "비우기" |
| `domain/ma-domain-core/.../member/domain/ProfileChanges.kt` | `nickname: String?`, `region: Region?`, `highSchool: Changed<String>?`, `university: Changed<String>?`. raw 값을 받아 내부에서 VO 생성 |
| `domain/ma-domain-core/.../member/domain/MemberProfile.kt` | 응답용 도메인 결과 객체. `member: Member` + `profileImageUrl: String?`. 팩토리 `of(member, profileImageUrl)` |
| `domain/ma-domain-core/.../member/domain/MemberValidator.kt` | `@Component`. `validateNicknameAvailable(current: Member, nickname: String?)` — 닉네임이 바뀌지 않으면 검사 생략, 바뀌었으면 `existsByNickname` 조회 후 중복이면 `DuplicateException` |
| `domain/ma-domain-core/.../member/application/MemberProfileService.kt` | 클래스 `@Transactional(readOnly = true)`. `findOne(memberId): MemberProfile` / `update(memberId, changes): MemberProfile` (`@Transactional` 재선언). flat 위임만 |

#### 수정

| 파일 | 변경 |
|---|---|
| `domain/.../member/domain/Member.kt` | `nickname` · `region` · `highSchool` · `university` 를 `var ... private set` 으로 전환. 수정 행위 `changeProfile(changes: ProfileChanges)` 추가 — 변경 지시를 해석해 자기 상태를 바꾼다 |
| `domain/.../member/domain/port/MemberCommandRepository.kt` | `updateProfile(member: Member)` 추가. `changeProfile` 이 끝난 Member 의 4개 컬럼을 그대로 쓴다(조건부 SQL 불필요) |
| `infrastructure/storage/ma-db-core/.../member/dao/MemberCommandDao.kt` | `updateProfile` 구현 |
| `infrastructure/storage/ma-db-core/.../member/repository/MemberCoreRepository.kt` | 위임 추가 |
| `boot/ma-boot-web/.../config/SecurityConfig.kt` | D4 permitAll 1줄 (Part C) |

### Part B — 사진 URL 규약 전환

#### 신규

| 파일 | 내용 |
|---|---|
| `domain/.../member/domain/photo/MemberPhotoUrlResolver.kt` | `@Component`, `FileUrlResolver` 주입. `resolve(photo: MemberPhoto?): String?` (썸네일 우선, 없으면 원본), `resolveByMember(photos: MemberPhotos): ProfileImageUrls` |
| `domain/.../member/domain/photo/ProfileImageUrls.kt` | 일급 컬렉션. `data: Map<Long, String>`, `urlOf(memberId): String?` |
| `.../resources/script/migration/20260820_member_photo_storage_key.sql` | 컬럼 리네임 + 기존 값 접두사 제거 + 닉네임 유니크 (§9) |

#### 수정

| 파일 | 변경 |
|---|---|
| `domain/.../member/domain/photo/MemberPhotoProcessor.kt` | `storeOriginal` · `storeThumbnail` 이 상대 storageKey 를 반환하도록 `toRelativeKey` 도입 (`MediaProcessor` 와 동일 형태). `deleteFiles` 를 `fileStorage.delete` → `deleteByKey` 로 |
| `domain/.../member/domain/photo/MemberPhoto.kt` · `NewPhoto.kt` · `ProcessedPhoto.kt` | `filePath`→`storageKey`, `thumbnailPath`→`thumbnailKey` 로 리네임. `hasThumbnail()` 유지 |
| `infrastructure/.../member/entity/table/MemberPhotoTable.kt` · `entity/MemberPhotoEntity.kt` · `dao/MemberPhotoCommandDao.kt` · `dao/MemberPhotoQueryDao.kt` | 컬럼 `STORAGE_KEY` · `THUMBNAIL_KEY` 로 변경 및 매핑 갱신 |
| `domain/.../matching/domain/MatchingResults.kt` | `combineWithProfiles` · `toClaimerProfiles` 가 `MemberPhotos` 대신 `ProfileImageUrls` 를 받는다. `photo?.thumbnailPath` → `imageUrls.urlOf(id)` |
| `domain/.../matching/application/MatchingResultQueryService.kt` | `MemberPhotoUrlResolver` 주입, 조회한 `MemberPhotos` 를 `ProfileImageUrls` 로 변환해 전달 |
| `domain/.../withdrawal/domain/MemberWithdrawalBackup.kt` | 백업 스냅샷의 photo 필드가 key 값을 담게 됨 — 구조 변경 없음, 값 의미만 바뀐다(주석 갱신) |
| `.../resources/script/ddl.sql` | `MEMBER_PHOTOS` 컬럼명, `MEMBERS.NICKNAME` 유니크 |
| `.../resources/dummy-data/01_members.sql` | 컬럼명 변경 반영 |

### Part C — 부수

| 파일 | 변경 |
|---|---|
| `boot/ma-boot-web/.../config/SecurityConfig.kt` | `.requestMatchers(HttpMethod.GET, "/files/member/**").permitAll()` 추가 |
| `infrastructure/.../member/entity/table/MemberTable.kt` | `nickname` 에 `.uniqueIndex()` |

---

## 6. 트랜잭션 경계

- `GET` — `@Transactional(readOnly = true)`. 회원 1행 + 사진 1행 조회.
- `PATCH` — `MemberProfileService.update` 단일 트랜잭션. 순서: 회원 조회 → `MemberValidator.validateNicknameAvailable` → `member.changeProfile(changes)` → `updateProfile(member)` → 사진 조회 후 `MemberProfile` 조립.
- 파일 I/O 없음 — 사진 업로드는 기존 `MemberPhotoService` 경로 그대로.

---

## 7. 예외 → HTTP 매핑

| 상황 | 예외 | 상태 |
|---|---|---|
| 닉네임 형식 위반 | Bean Validation | 400 |
| `nickname`·`region` 에 명시적 null | Bean Validation / 역직렬화 | 400 |
| 알 수 없는 `region` enum 코드 | Jackson | 400 |
| 닉네임 중복 | `DuplicateException` | 409 |
| 토큰 없음·만료 | 기존 시큐리티 | 401 |
| 존재하지 않는 회원 | 기존 `findOne` 경로 | 404 |

전부 기존 `GlobalExceptionHandler` 매핑을 재사용한다 — 신규 예외 타입 없음.

---

## 8. 테스트

Service 빈 주입 중간레벨 통합테스트는 만들지 않는다. API→DB E2E 와 순수 단위 테스트만 쓴다.

### API E2E (`MemberProfileApiTest`, REST Docs 겸용)

- GET: 전 필드가 내려온다 / 사진 없으면 `profileImageUrl` 이 null / 사진 있으면 `/files/member/...` 형태 URL
- PATCH: 4필드 전부 전달 시 전부 반영
- PATCH: `nickname` 만 전달 시 나머지 3개가 그대로 유지된다 (부분 수정 핵심)
- PATCH: `highSchool: null` 전달 시 DB 가 null 이 된다 (비우기)
- PATCH: `highSchool` 생략 시 기존 값이 유지된다 (생략 ≠ 비우기 — D3-a 검증)
- PATCH: 닉네임 형식 위반 → 400 / 다른 회원이 쓰는 닉네임 → 409 / 자기 닉네임 그대로 → 200
- 인증 없음 → 401

DB 상태 검증은 `transaction { }` 으로 테이블을 직접 읽는다.

### 단위 테스트

- `MemberTest` — `changeProfile` 이 생략·비우기·변경 세 경우를 올바로 해석하는가
- `ChangedTest` — tri-state 표현
- `MemberValidatorTest` — 닉네임 미변경 시 조회를 하지 않는가(Mockk `verify(exactly = 0)`), 중복 시 `DuplicateException`
- `MemberPhotoUrlResolverTest` — 썸네일 우선, 썸네일 없으면 원본, 사진 없으면 null
- `MemberPhotoProcessorTest` — 상대 key 반환으로 기대값 갱신
- `MatchingResultsTest` — `ProfileImageUrls` 기반으로 기대값 갱신

### 회귀 확인 (기존 테스트 수정 필요)

`MemberPhotoApiTest` · `MemberPhotoServiceTest` · `MemberPhotoCommandDaoTest` · `MemberPhotoQueryDaoTest` · `MatchingResultQueryApiTest` · `ClaimerQueryApiTest` · `MatchingResultQueryServiceTest` — 컬럼·필드 리네임과 URL 형태 변경 반영.

---

## 9. 데이터 마이그레이션

`script/migration/20260820_member_photo_storage_key.sql` — 기존 DB 대상. 신규 DB 는 `ddl.sql` 만 실행하면 된다.

1. **닉네임 중복 사전 확인** — 유니크 적용 전 반드시 선행. 중복이 있으면 정리 방식을 사용자에게 물어야 하므로, 스크립트에는 확인 쿼리와 주석만 넣고 **정리 문은 넣지 않는다**.
   `SELECT NICKNAME, COUNT(*) FROM MEMBERS WHERE DELETED = false GROUP BY NICKNAME HAVING COUNT(*) > 1;`
2. `MEMBER_PHOTOS` 컬럼 리네임 — `FILE_PATH`→`STORAGE_KEY`, `THUMBNAIL_PATH`→`THUMBNAIL_KEY`
3. 기존 값에서 basePath 접두사 제거 — `uploads/` 로 시작하는 값의 앞 8자를 벗긴다. 운영 basePath 가 `uploads` 가 아니면 스크립트 상단 변수만 바꾼다
4. `MEMBERS.NICKNAME` 유니크 인덱스 추가 (1번 확인 통과 후)

⚠ 3번은 **되돌리기 어렵다.** 실행은 배포 시점에 사용자 판단으로 한다 — 이 PR 은 스크립트 준비까지만 한다.

---

## 10. REST Docs / 문서 동기화

- `MemberVocabulary.kt` 에 필드 정의 추가 — `nickname` · `name` · `gender` · `birthDate` · `phoneNumber` · `region` · `highSchool` · `university` (`memberId` · `profileImageUrl` 은 이미 있음)
- `src/docs/asciidoc/` 에 회원 프로필 snippet 추가 후 `main.adoc` 에 연결
- `docs/api-todo.md` — 완료된 API > 회원 테이블에 2건 추가. 별건 2개를 TODO 에 기록: ① `MEMBERS.PROFILE_IMAGE_URL` 죽은 컬럼 제거 ② 프로필 사진 `APPROVAL_STATUS` 승인 워크플로 미구현

---

## 11. 프론트 회신 초안 (`to-backend.md` REQ-016 Backend Reply)

- 구현: `GET /api/members/me`, `PATCH /api/members/me`
- **계약 축소 알림**: PATCH 대상에서 `name` · `gender` 를 제외했다. 두 값은 매칭 후보를 거르는 하드 필터라 프로필 화면에서 바꾸면 "나를 찾던 사람이 나를 못 찾게" 되고, 이미 등록된 TargetInfo 의 `targetGender` 가 낡은 값으로 남는다. 변경이 필요하면 별도 요구사항으로 올려 달라 — 재동기화 정책을 함께 정해야 한다.
- PATCH 는 부분 수정이다. 보내지 않은 필드는 유지되고, `highSchool`·`university` 에 `null` 을 명시하면 비워진다.
- `region` 선택지는 기존 `GET /api/members/regions` 를 그대로 쓰면 된다. `gender` 는 `MALE`/`FEMALE` 2개뿐이라 별도 API 를 만들지 않았다.
- `profileImageUrl` 은 `/files/member/...` 형태이며 **비인증 접근 가능**하다 — `Image.network` 에 헤더를 붙이지 않아도 된다.
- 참고: `lib/api/member_api.dart` 의 `getMember()` 는 존재하지 않는 `GET /api/members/{memberId}` 를 부른다. 호출처가 없으니 제거하거나 `/me` 로 교체하면 된다.
- 프론트가 memberId 를 갖고 있지 않아 `/me` 로 설계했다. 응답의 `memberId` 는 인코딩된 값이다.

---

## 12. 작업 순서

브랜치 `feat/member-profile-me` (base `develop`).

1. Part B 먼저 — 저장 규약 전환. 기존 테스트가 RED 로 떨어지는 것부터 관측하고 GREEN 으로 만든다. (Part A 가 이 위에 얹히므로 순서를 뒤집으면 두 번 고치게 된다)
2. Part A 스켈레톤 — public 타입·시그니처만, 본문은 `TODO("구현")`
3. Part A 테스트 선작성 → RED 관측
4. Part A 구현 → GREEN
5. Part C — permitAll, 유니크 인덱스, ddl.sql
6. 마이그레이션 스크립트 작성 (실행하지 않는다)
7. `code-reviewer` 리뷰 → 지적 반영
8. REST Docs 생성 및 `main.adoc` 연결
9. 실기동 검증 — 로컬 서버 띄워 GET/PATCH 실 호출, 사진 URL 을 브라우저로 직접 열어 200 확인
10. `docs/api-todo.md` 동기화, 프론트 회신 초안 정리
11. 커밋 → 푸시 → **Draft PR** (base `develop`) + 리뷰 페이지 HTML

---

## 13. 리스크 / 미결

| # | 항목 | 대응 |
|---|---|---|
| 1 | 기존 닉네임 중복 데이터가 있으면 유니크 인덱스를 못 건다 | §9-1 확인 쿼리 결과를 보고 사용자에게 정리 방식을 묻는다. 중복이 있으면 Part C 의 유니크만 이번 PR 에서 빼고 나머지는 진행한다 |
| 2 | `Optional<String>?` 역직렬화가 이 프로젝트 Jackson 설정에서 의도대로 동작하는지 | 스켈레톤 단계에서 역직렬화 테스트 1개로 **먼저 확인**한다. 실패하면 대안은 커스텀 `JsonDeserializer` 또는 "빈 문자열 = 비우기" 계약 |
| 3 | `MEMBER_PHOTOS` 컬럼 리네임이 운영 데이터에 영향 | 마이그레이션은 준비만, 실행은 배포 시 사용자 판단 (§9) |
| 4 | 매칭 응답의 `profileImageUrl` 형태가 바뀐다 (경로 → URL) | 프론트가 지금 그 값을 쓰고 있는지 확인 필요 — 어차피 깨진 값이라 쓰고 있을 가능성은 낮지만, 회신에 명시한다 |
| 5 | `Member` 의 4개 필드가 `var` 가 된다 | `withdrawalRequestedAt` 과 동일하게 `private set` + 도메인 메서드로만 변경 가능하게 해 캡슐화를 유지한다 |
