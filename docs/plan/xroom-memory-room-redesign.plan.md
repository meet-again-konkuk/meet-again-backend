# Plan: X룸 전면 재설계 → 기억의 방(Memory Room)

> 작성일: 2026-06-25
> 상태: Draft

## 1. 개요

기존 theme 기반 단순 X룸 모델을 **전부 걷어내고**, 프론트 신스펙(REQ-006/008/009/010, `lib/api/xroom_api.dart`)대로 **"기억의 방" 3계층 애그리거트**(Xroom → Memory → Media)로 재구축한다.

- 엔드포인트 경로는 기존대로 `/api/xrooms` 유지(엔드포인트 10종).
- **새 도메인을 만들지 않는다.** 기존 `xroom` 도메인 안에 Xroom/Memory/Media를 둔다(같은 애그리거트 컨텍스트).
- 모든 ID는 응답에서 암호화(`@EncryptId`), 경로/파라미터는 복호화(`@DecryptId`). 모든 변경/삭제는 **방 작성자(owner)만**, 수신자(recipient)는 **조회만**. 전부 soft delete.
- 대규모 변경이므로 **Phase 0~4 단계별 PR**로 쪼갠다(§9).

> **네이밍/구조 규칙(확정).** 애그리거트 루트 클래스명은 **`Xroom`** 그대로 유지한다(패키지 `xroom`·테이블 `XROOMS`·경로 `/api/xrooms`·`ObfuscationType.XROOM`·`EntityType.XROOM`·프론트 계약이 모두 "xroom"이라 일관성 유지). `Room`으로 리네임하지 않는다. 루트는 `xroom/domain/` **직속**(서브패키지 없음). 하위 엔티티 Memory/Media는 `member/domain/photo` 선례대로 `xroom/domain/memory`·`xroom/domain/media` 서브패키지로 둔다. **아래 본문에서 "Room\*"으로 표기된 루트 타입(RoomTitle/RoomTemplate/RoomValidator/Room\*Repository 등)은 모두 `Xroom*`(XroomTitle/XroomTemplate/XroomValidator/Xroom\*Repository)로 읽는다.**

### 도메인 분리 판단

- Xroom은 애그리거트 루트(`xroom/domain/` 직속), Memory/Media는 하위 엔티티(`xroom/domain/memory`·`media` 서브패키지) — **하나의 애그리거트 컨텍스트**.
- 매칭 도메인의 `findClaimedByTarget`를 **읽기 전용으로 재활용**(수신자 판정)하되, xroom이 매칭 도메인에 의존하는 방향은 기존 `MatchingResultQueryService → XroomQueryRepository` 의존과 반대다. 수신자 판정은 매칭 포트를 xroom application에서 주입받아 조합한다(§7 참조).

---

## 2. API 설계

엔드포인트 10종. base path `/api/xrooms`. 인증 전부 필요(`@LoginMember`). roomId/memoryId/mediaId는 경로 `@DecryptId`, 응답 `@EncryptId`.

| # | Method | Endpoint | 권한 | 성공 | 용도 |
|---|--------|----------|------|------|------|
| 1 | POST | `/api/xrooms` | 작성자 | 201 | 방 생성. body `{targetInfoId, finalMessage?}` → `{roomId}` |
| 2 | GET | `/api/xrooms/received` | 수신자 | 200 | 내가 수신(초대)한 방 목록 → `{rooms:[{id, title, senderName, memoryCount}]}` |
| 3 | GET | `/api/xrooms/me` | 작성자 | 200 | 내가 만든 방 목록 → `{rooms:[{id, title, recipientName, targetInfoId, memoryCount, updatedAt}]}` |
| 4 | GET | `/api/xrooms/{roomId}` | 작성자 or 수신자 | 200 | 방 상세 + memories[] (시점 오름차순) |
| 5 | PATCH | `/api/xrooms/{roomId}` | 작성자 | 200 | finalMessage 수정/삭제. body `{finalMessage}` → `{roomId}` |
| 6 | POST | `/api/xrooms/{roomId}/memories` | 작성자 | 201 | 기억 추가 → `{memoryId}` |
| 7 | PATCH | `/api/xrooms/{roomId}/memories/{memoryId}` | 작성자 | 200 | 기억 수정 → `{memoryId}` |
| 8 | DELETE | `/api/xrooms/{roomId}/memories/{memoryId}` | 작성자 | 200 | 기억 soft delete(+media 연쇄) → `{memoryId, deleted:true}` |
| 9 | POST | `/api/xrooms/{roomId}/memories/{memoryId}/photo` | 작성자 | 201 | 사진 업로드/교체(multipart `photo`) → `{mediaId, photoUrl, thumbnailUrl?}` |
| 10 | DELETE | `/api/xrooms/{roomId}/memories/{memoryId}/photo` | 작성자 | 200 | active 사진 soft delete → `{memoryId, photoDeleted:true}` |

### 주요 DTO 계약

- **GET `/{roomId}`**: `{id, title, recipientName, template, finalMessage?, memories:[{id, title, eventDate, eventDatePrecision, location?, emotionTags[], text?, letter?, photoUrl?}]}`. `eventDate`는 precision별 wire 포맷 문자열(YEAR `"2019"` / MONTH `"2019-05"` / DAY `"2019-05-10"`).
- **POST/PATCH memory body**: `{title, eventDate, eventDatePrecision, location?, emotionTags[], text?, letter?}`. `text ⊕ letter`(둘 중 하나만, 상호배타).
- **빈 결과**: 목록은 `{rooms:[]}`, memories 없으면 `[]`, memoryCount 0.

### URL 중첩 정책 메모

`/api/xrooms/{roomId}/memories/{memoryId}/photo`는 3단계 중첩으로 code-implementation-rules §14("2단계까지")를 초과한다. 단일 사진 리소스이므로 `photo`를 메모리의 종속 행위로 본다. 트레이드오프는 §10 확인필요로 남긴다.

---

## 3. 아키텍처

```
┌───────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web  (xroom/api)                                          │
│   XroomCommandApi   : POST /xrooms, PATCH /{roomId}                     │
│   XroomQueryApi     : GET /received, /me, /{roomId}                     │
│   MemoryCommandApi  : POST/PATCH/DELETE /{roomId}/memories[/{id}]       │
│   MemoryPhotoApi    : POST/DELETE /{roomId}/memories/{id}/photo         │
│   request/ ・ response/  (DTO, @Encrypt/@DecryptId)                     │
└───────────────┬───────────────────────────────────────────────────────┘
                │ (Service 호출만)
┌───────────────▼───────────────────────────────────────────────────────┐
│ domain/ma-domain-core  (xroom/application, xroom/domain)               │
│   application/                                                          │
│     XroomCommandService   create / updateFinalMessage                  │
│     XroomQueryService      findMine / findReceived / findDetail        │
│     MemoryCommandService   addMemory / updateMemory / removeMemory      │
│     MemoryPhotoService     uploadPhoto / removePhoto                    │
│   domain/                                                              │
│     room/   Room, NewRoom, RoomTitle, FinalMessage, RoomTemplate,      │
│             RoomValidator, port/{RoomCommand,RoomQuery}Repository       │
│     memory/ Memory, NewMemory, Memories, EventDate(VO), EmotionTags,   │
│             MemoryContent(text⊕letter), port/{MemoryCommand,Query}Repo  │
│     media/  Media, NewMedia, port/{MediaCommand,Query}Repository        │
│   ── 재활용(주입) ──                                                    │
│     MatchingResultRepository.findClaimedByTarget (수신자 판정)          │
│     TargetInfoQueryRepository.findOne            (소유권/recipientName) │
│     MemberQueryRepository.findByIds              (senderName 파생)      │
│     FileStorage(port), ThumbnailGenerator(port)  (사진 저장/썸네일)     │
└───────────────┬───────────────────────────────────────────────────────┘
                │ (implements)
┌───────────────▼───────────────────────────────────────────────────────┐
│ infrastructure/storage/ma-db-core  (xroom/{entity,dao,repository})     │
│   XroomTable(스키마 교체) / MemoryTable / MemoryMediaTable               │
│   / MemoryEmotionTagTable                                               │
│   XroomEntity / MemoryEntity / MediaEntity  (+ toDomain/from)          │
│   *CommandDao / *QueryDao  →  *CoreRepository (@Repository)            │
│ infrastructure/support/ma-file-storage  (재활용)                       │
│   LocalFileStorage(@Value file.upload.base-path), ThumbnailGenerator    │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 4. 데이터 모델 (3계층 애그리거트)

### 4.1 Room (애그리거트 루트)

- 필드: `ownerId`(작성자), `targetInfoId`, `finalMessage?`, `template`(서버 기본값), `title`(서버 기본값/파생) + BaseTable audit/softDelete.
- 파생값(저장 안 함): `recipientName` = `TargetInfo.targetName`, `senderName` = owner 회원명(`Member.name`).
- 행위: `validateOwnership(memberId)`(작성자 검증), `updateFinalMessage(FinalMessage)`(null/빈문자열로 삭제).

### 4.2 Memory (하위 엔티티)

- 필드: `roomId`, `title`, `eventDate`+`eventDatePrecision`(VO `EventDate`), `location?`, `emotionTags[]`(일급컬렉션 `EmotionTags`), `content`(VO `MemoryContent` = text ⊕ letter) + softDelete.
- 정렬: 시점 오름차순(`EventDate` 정규화 DATE + precision 기준).
- 행위: `update(...)`, `MemoryContent`가 text/letter 상호배타 검증.

### 4.3 Media (하위 엔티티)

- 필드: `memoryId`, `storageKey`, `originalFilename`, `mimeType`, `fileSize`, `thumbnailKey?`, `createdAt` + softDelete. **메타만 저장, 절대경로 금지.**
- 기억당 active 1장. 교체 = 기존 active soft delete + 새 active insert.
- `photoUrl`/`thumbnailUrl`은 `storageKey`로부터 서빙 계층에서 조립(저장 안 함).

### 4.4 부분 날짜 VO — `EventDate`

- 위치: `domain/.../xroom/domain/memory/EventDate.kt`. 원시값 포장 규칙(§code-implementation-rules 3).
- 구성: `precision: EventDatePrecision(YEAR|MONTH|DAY)` + 정규화된 `LocalDate`.
- 정규화 규칙: YEAR→`yyyy-01-01`, MONTH→`yyyy-MM-01`, DAY→실제일자. **DB에 정규화 DATE + precision 2컬럼 저장** → `ORDER BY EVENT_DATE ASC`로 시점 정렬(애플리케이션 정렬 회피, 인덱스 활용).
- wire 변환(VO 내부): `toWire()` → precision별 `"2019"`/`"2019-05"`/`"2019-05-10"`. 역변환 `parse(value, precision)` 팩토리.
- 검증: precision과 wire 포맷 자릿수 일치, 유효 날짜 여부를 `init`/팩토리에서 검증.

### 4.5 `EmotionTags` (일급 컬렉션)

- 위치: `domain/.../xroom/domain/memory/EmotionTags.kt`. 멤버 변수명 `val data`(규칙 §4).
- 프론트 프리셋(행복/설렘/그리움/고마움/미안함/따뜻함/웃음/추억) + 기타 태그 혼합. **백엔드는 저장/반환만**("행복" 필터는 프론트). 빈 리스트 허용 여부는 §10 확인필요.

### 4.6 `MemoryContent` (text ⊕ letter VO)

- 위치: `domain/.../xroom/domain/memory/MemoryContent.kt`. `init`에서 둘 다 있거나 둘 다 없는 경우 검증(상호배타). 둘 다 없음 허용 여부는 §10 확인필요(추천: 최소 하나 필수).

---

## 5. DDL (FK 금지 — PK/INDEX만)

파일: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`. 모든 테이블 BaseTable 공통 컬럼(CREATED_*, LAST_MODIFIED_*, DELETED, DELETED_DATE, DELETED_BY) 포함.

### 5.1 XROOMS (스키마 교체)

- **제거**: `THEME`.
- **추가**: `TEMPLATE VARCHAR(32) NOT NULL DEFAULT 'chat_memory'`, `TITLE VARCHAR(100) NOT NULL`, `FINAL_MESSAGE VARCHAR(1000) NULL`.
- **유지**: `XROOM_ID`(PK), `OWNER_ID`, `TARGET_INFO_ID`.
- 인덱스: `INDEX idx_xroom_owner_id (OWNER_ID)`, `INDEX idx_xroom_target_info_id (TARGET_INFO_ID)` (조회용). **DB UNIQUE 두지 않음(§10-3 옵션 B 확정)** — "targetInfo당 active Room 1개"는 앱 레벨 `RoomQueryRepository.exists(targetInfoId)`(활성 행만 카운트)로 보장. soft-deleted 방은 exists()가 무시하므로 재생성 자동 허용.

### 5.2 MEMORIES (신규)

- 컬럼: `MEMORY_ID`(PK), `ROOM_ID BIGINT NOT NULL`, `TITLE VARCHAR(200) NOT NULL`, `EVENT_DATE DATE NOT NULL`(정규화), `EVENT_DATE_PRECISION VARCHAR(8) NOT NULL`, `LOCATION VARCHAR(200) NULL`, `TEXT TEXT NULL`, `LETTER TEXT NULL`.
- 인덱스: `INDEX idx_memory_room_event (ROOM_ID, EVENT_DATE)` — 방별 조회 + 시점 정렬 동시 충족.

### 5.3 MEMORY_MEDIA (신규)

- 컬럼: `MEDIA_ID`(PK), `MEMORY_ID BIGINT NOT NULL`, `STORAGE_KEY VARCHAR(512) NOT NULL`, `ORIGINAL_FILENAME VARCHAR(255) NOT NULL`, `MIME_TYPE VARCHAR(100) NOT NULL`, `FILE_SIZE BIGINT NOT NULL`, `THUMBNAIL_KEY VARCHAR(512) NULL`.
- 인덱스: `INDEX idx_media_memory (MEMORY_ID)`. active 1장 정책은 애플리케이션(교체 시 기존 soft delete)으로 보장 — partial unique 불가하므로 DB UNIQUE는 두지 않음(§10-5).

### 5.4 MEMORY_EMOTION_TAGS (신규 — 자식 테이블 확정, §10-4)

- 컬럼: `TAG_ID`(PK), `MEMORY_ID BIGINT NOT NULL`, `TAG VARCHAR(50) NOT NULL` + BaseTable 공통.
- 인덱스: `INDEX idx_emotion_memory (MEMORY_ID)`. FK 미사용.
- 도메인은 일급 컬렉션 `EmotionTags`(`val data`)로 포장. 저장: 기억 저장/수정 시 기존 태그 행 정리 후 재삽입(또는 diff). 조회: `MEMORY_ID`로 일괄 조회해 Memory에 합성.

---

## 6. 변경 전략

| 레이어 | 현재(구 X룸) | 변경 후(기억의 방) | 변환 위치 |
|--------|------|---------|-----------|
| Domain Model | `Xroom(theme)` / `NewXroom` / `XroomTheme` | `Room(template,title,finalMessage)` / `NewRoom` / `RoomTemplate` / `FinalMessage` VO. Memory/Media/EventDate/EmotionTags/MemoryContent 신규 | `xroom/domain/{room,memory,media}` |
| Validator | `XroomValidator.validate(NewXroom)`(중복 검증) | `RoomValidator.validate(NewRoom)`(소유권+중복) 유지·확장 | `xroom/domain/room` |
| Command Port | `XroomCommandRepository(save, delete(ownerId))` | `RoomCommandRepository(save, updateFinalMessage, softDelete(ownerId))` + `MemoryCommandRepository` + `MediaCommandRepository` | `xroom/domain/*/port` |
| Query Port | `XroomQueryRepository(exists, exists(Set), find(ownerId))` | `RoomQueryRepository`에 `exists`/`exists(Set)`/`find(ownerId)` **시그니처 보존**(매칭 hasXroom·탈퇴백업 호환) + `findOne(roomId)`, `findByTargetInfoIds(Set)`(received) 추가. `MemoryQueryRepository`(find(roomId), countByRooms), `MediaQueryRepository` 신규 | `xroom/domain/*/port` |
| Service | `XroomCommandService.create(targetInfoId, memberId)` | Command/Query 분리 + Memory/Photo 서비스 신규(§3) | `xroom/application` |
| Infra | `XroomTable(theme)` / Entity / Dao / CoreRepository | Table 스키마 교체 + Memory/Media/EmotionTag Table·Entity·Dao·CoreRepository 신규 | `xroom/{entity,dao,repository}` |
| Boot | `XroomCommandApi`(POST `?targetInfoId` 쿼리) / `CreateXroomResponse` | POST는 **body** `{targetInfoId, finalMessage?}` 방식으로 변경. Query/Memory/Photo Api + request/response DTO 신규 | `xroom/api` |
| 공통 enum | `ObfuscationType(XROOM)`, `EntityType(XROOM)` | `MEMORY`, `MEDIA` 추가. `StorageDomainType`에 `MEMORY` 추가, `StorageUsageType`에 `MEMORY_PHOTO` 추가 | 해당 enum 파일 |
| 외부 의존 | `MatchingResultQueryService`(hasXroom), `MemberDataCleaner.cleanXroom`, `MemberWithdrawalBackupCollector` | 포트 시그니처 보존으로 컴파일 정합화. cleaner는 Room+하위 일괄 정리로 확장 | 해당 파일 |

### 변환 규칙 요지

- **Request DTO → 도메인 변환은 도메인 모듈 내부에서**: API의 request DTO는 String/원시값만 받고, `FinalMessage`/`EventDate`/`EmotionTags`/`MemoryContent` 등 VO 생성은 Command/도메인 모델이 String을 받아 내부에서 수행한다(프로젝트 규칙: Request DTO에서 VO 직접 생성 금지).
- **단건 조회 포트는 non-null**: `RoomQueryRepository.findOne(roomId)`는 없으면 CoreRepository에서 예외(`EntityType.XROOM`). nullable 필요 시 `OrNull` 접미사.
- **메서드 네이밍**: 파라미터로 유추 가능한 조건은 메서드명에 반복 금지(`exists(targetInfoId)`, `find(ownerId)`). 같은 타입 다른 조건일 때만 `findByXxx` 접미사 허용.

---

## 7. 수신자(received) 판정 — 매칭 기반 (추천안 A 채택)

`GET /api/xrooms/received`는 별도 초대 엔티티 없이 매칭 결과를 재활용한다.

**claim 메커니즘은 이미 구현돼 있다**(추가 결정 불필요). `MatchingResult.claimed` 필드 + `claim()` 액션, `PATCH /api/matching-results/{id}/claim`, `GET /api/claimers/me`(ClaimerQueryApi)가 존재하며, `MatchingResultQueryDao.findClaimedByTarget(memberId)`는 이미 `targetId == memberId AND claimed == true`로 필터한다. 따라서 수신자 = "등록자가 claim한 매칭에서 내가 target인 사람"으로 **코드상 이미 확정**돼 있고, received 방 판정은 이 쿼리를 그대로 재사용하면 된다.

흐름(`XroomQueryService.findReceived(memberId)`):
1. `matchingResultRepository.findClaimedByTarget(memberId)` → 내가 target이면서 등록자가 **claim한** 매칭 결과 리스트(claimed=true 이미 강제).
2. `MatchingResults(...).extractTargetInfoIds()` → 그 targetInfoId 집합.
3. `roomQueryRepository.findByTargetInfoIds(targetInfoIds)` → 해당 targetInfo의 active Room 목록.
4. owner들의 회원명 일괄 조회(`memberQueryRepository.findByIds(ownerIds)`) → `senderName` 파생, `memoryCount`는 `memoryQueryRepository.countByRooms(roomIds)` 벌크 조회(N+1 방지).
5. Room 상세 접근 권한(GET `/{roomId}`)도 "작성자 or 수신자"이므로 동일 판정 로직(작성자=ownerId, 수신자=findClaimedByTarget 결과의 room)을 권한 검사에 재사용.

---

## 8. 상세 설계 (파일별, 시그니처만)

> 코드 스니펫 없이 파일 경로 + 시그니처 한 줄 + 규칙. 패키지 루트 `com.konkuk.ma.domain.xroom`.

### 8.1 Domain — Xroom (애그리거트 루트, `xroom/domain/` 직속) — **Phase 0에서 구현 완료**

> Phase 0(PR #31)에서 아래를 이미 구현했다. 루트 클래스명은 `Xroom`(서브패키지 없음). findOne/findByTargetInfoIds/updateFinalMessage 등 신규 조회·갱신은 Phase 1.

- `XroomTemplate.kt` (enum): `CHAT_MEMORY`, wire 값 `"chat_memory"`, 기본값 상수. ✅
- `XroomTitle.kt` (VO): 기본값 상수("기억의 방"), 길이 검증. ✅
- `FinalMessage.kt` (VO): nullable 의미 포장(`of(value: String?)`→null/빈문자열은 null). 길이 검증. ✅
- `NewXroom.kt`: `NewXroom(ownerId: Long, targetInfoId: Long, finalMessage: String? = null)`, 내부에서 `FinalMessage`/`XroomTitle`/`XroomTemplate` 기본값 조립. ✅
- `Xroom.kt`: `Xroom(id, ownerId, targetInfoId, title: XroomTitle, template: XroomTemplate, finalMessage: FinalMessage?, createdDate)`. `fun validateOwnership(memberId)`, `fun updateFinalMessage(FinalMessage?)`. (권한 조합은 Service) ✅
- `XroomValidator.kt`: `fun validate(newXroom: NewXroom)` — targetInfo 소유권 + active Xroom 중복 검증. ✅
- `port/XroomCommandRepository.kt`: `save(newXroom): Long`, `delete(ownerId)`. (Phase 1에서 `updateFinalMessage(xroom)` 추가) ✅
- `port/XroomQueryRepository.kt`: `exists(targetInfoId): Boolean`, `exists(targetInfoIds: Set<Long>): Set<Long>`(매칭 hasXroom 호환), `find(ownerId): List<Xroom>`(탈퇴백업 호환). (Phase 1에서 `findOne(xroomId): Xroom`, `findByTargetInfoIds(Set): List<Xroom>` 추가) ✅

### 8.2 Domain — Memory

- `memory/EventDatePrecision.kt` (신규, enum): `YEAR, MONTH, DAY`.
- `memory/EventDate.kt` (신규, VO): `EventDate(precision, normalizedDate)`. 팩토리 `parse(value: String, precision: EventDatePrecision)`, `fun toWire(): String`, `fun sortKey(): LocalDate`(= normalizedDate).
- `memory/EmotionTags.kt` (신규, 일급컬렉션): `val data: List<String>`. 팩토리 `of(tags)`.
- `memory/MemoryContent.kt` (신규, VO): `MemoryContent(text: String?, letter: String?)`, `init`에서 상호배타 검증.
- `memory/NewMemory.kt` (신규): `NewMemory(roomId, title, eventDate: String, eventDatePrecision, location?, emotionTags: List<String>, text?, letter?)` — 내부에서 `EventDate.parse`/`EmotionTags.of`/`MemoryContent` 조립.
- `memory/Memory.kt` (신규): 필드 + `fun update(...)`(작성자 검증은 Service가 Room 통해 수행). `fun toView(photoUrl: String?)` 또는 응답 조립은 Service.
- `memory/Memories.kt` (신규, 일급컬렉션): `val data`, `fun sortedByEventDate(): List<Memory>`(또는 정렬은 쿼리에서). `fun count(): Int`.
- `memory/port/MemoryCommandRepository.kt` (신규): `save(newMemory): Long`, `update(memory)`, `softDelete(memoryId)`.
- `memory/port/MemoryQueryRepository.kt` (신규): `findOne(memoryId): Memory`, `find(roomId): List<Memory>`(EVENT_DATE ASC), `countByRooms(roomIds: Set<Long>): Map<Long, Int>`(received/me memoryCount 벌크).

### 8.3 Domain — Media

- `media/NewMedia.kt` (신규): `NewMedia(memoryId, storageKey, originalFilename, mimeType, fileSize, thumbnailKey?)`.
- `media/Media.kt` (신규): 필드 + softDelete 의미. `fun toUrl(baseUrl): String`(서빙 계층 협력) — 또는 URL 조립은 별도 컴포넌트.
- `media/port/MediaCommandRepository.kt` (신규): `save(newMedia): Long`, `softDeleteByMemory(memoryId)`, `softDeleteByMemories(memoryIds: Set<Long>)`(기억 연쇄 삭제).
- `media/port/MediaQueryRepository.kt` (신규): `findActiveByMemory(memoryId): Media?`(OrNull), `findActiveByMemories(memoryIds: Set<Long>): List<Media>`(상세 조회 photoUrl 벌크).

### 8.4 Domain — Application Service

- `application/XroomCommandService.kt` (수정): `create(targetInfoId, memberId, finalMessage: String?): Long`(RoomValidator 위임 → save), `updateFinalMessage(roomId, memberId, finalMessage: String?): Long`(findOne→validateOwnership→updateFinalMessage→port).
- `application/XroomQueryService.kt` (신규): `findMine(memberId): RoomSummaries`, `findReceived(memberId): ReceivedRoomSummaries`(§7), `findDetail(roomId, memberId): RoomDetail`(작성자 or 수신자 권한 → Room+Memories+활성 Media 조립). 의존: Room/Memory/MediaQueryRepository, MatchingResultRepository, TargetInfoQueryRepository, MemberQueryRepository.
- `application/MemoryCommandService.kt` (신규): `addMemory(roomId, memberId, command): Long`, `updateMemory(roomId, memoryId, memberId, command): Long`, `removeMemory(roomId, memoryId, memberId): Long`(memory soft delete + `mediaCommandRepository.softDeleteByMemory` 연쇄). 권한은 Room.validateOwnership.
- `application/MemoryPhotoService.kt` (신규, MemberPhotoService 패턴 재현): `uploadPhoto(roomId, memoryId, memberId, photoFile: PhotoFile): MediaUploadResult`(기존 active soft delete → FileStorage.store + ThumbnailGenerator → NewMedia.save), `removePhoto(roomId, memoryId, memberId)`(active media soft delete; **로컬 파일 즉시 삭제 안 함** → cleanup job 대상). 권한은 Room.validateOwnership.

> Command 객체(`AddMemoryCommand` 등)는 String 필드를 받아 도메인으로 전달(Request DTO에서 VO 생성 금지 규칙).

### 8.5 Domain — 공통 enum 수정

- `common/domain/id/ObfuscationType.kt`: `MEMORY("memory")`, `MEDIA("media")` 추가.
- `exception/EntityType.kt`: `MEMORY("Memory","id")`, `MEDIA("Media","id")` 추가.
- `common/domain/file/StorageDomainType.kt`: `MEMORY("memory")` 추가(사진 경로).
- `common/domain/file/StorageUsageType.kt`: `MEMORY_PHOTO("memory-photo")` 추가(필요 시).

### 8.6 Infrastructure — ma-db-core

- `xroom/entity/table/XroomTable.kt` (수정): `theme` 제거, `template`/`title`/`finalMessage(nullable)` 추가.
- `xroom/entity/table/MemoryTable.kt` (신규): `BaseTable("MEMORIES","MEMORY_ID")` + `roomId`/`title`/`eventDate(date)`/`eventDatePrecision`/`location(nullable)`/`text(text,nullable)`/`letter(text,nullable)`.
- `xroom/entity/table/MemoryMediaTable.kt` (신규): `BaseTable("MEMORY_MEDIA","MEDIA_ID")` + `memoryId`/`storageKey`/`originalFilename`/`mimeType`/`fileSize`/`thumbnailKey(nullable)`.
- `xroom/entity/table/MemoryEmotionTagTable.kt` (신규): `BaseTable("MEMORY_EMOTION_TAGS","TAG_ID")` + `memoryId`/`tag`.
- `xroom/entity/{XroomEntity,MemoryEntity,MediaEntity}.kt` (수정/신규): `toDomain()` + `companion from(row)`. MemoryEntity는 emotionTags 조인 결과를 받아 조립(또는 Dao에서 별도 조회 후 합성).
- `xroom/dao/{RoomCommandDao,RoomQueryDao,MemoryCommandDao,MemoryQueryDao,MediaCommandDao,MediaQueryDao}.kt` (수정/신규): `insertAndGetId`, `activeRows{}` 필터, `softDelete{}` 헬퍼(BaseTable). exists는 `limit(1).any()`, 단건은 `limit(1).firstOrNull()`. countByRooms는 `groupBy` 벌크.
- `xroom/repository/{RoomCommand,RoomQuery,MemoryCommand,MemoryQuery,MediaCommand,MediaQuery}CoreRepository.kt` (수정/신규): `@Repository`, 포트 구현, DAO 위임 + `toDomain()`만(로직 금지). 단건 없으면 예외.

### 8.7 Boot — ma-boot-web (xroom/api)

- `api/XroomCommandApi.kt` (수정): POST `@RequestBody CreateRoomRequest{targetInfoId(@DecryptId 불가 → String+Service 복호화 or body 복호 처리), finalMessage?}` → 201 `{roomId}`. PATCH `/{roomId}` body `{finalMessage}` → 200. (body 내 ID 복호화 방식은 §10-6).
- `api/XroomQueryApi.kt` (신규): GET `/received`, `/me`, `/{roomId}`(@PathVariable @DecryptId XROOM).
- `api/MemoryCommandApi.kt` (신규): POST/PATCH/DELETE `/{roomId}/memories[/{memoryId}]`.
- `api/MemoryPhotoApi.kt` (신규, MemberPhotoApi 패턴): POST consumes MULTIPART_FORM_DATA, `@RequestPart("photo")` → `PhotoFile.create`. DELETE.
- `api/request/*.kt`, `api/response/*.kt` (신규/수정): `CreateRoomRequest`, `UpdateFinalMessageRequest`, `MemoryRequest`, 응답 `RoomResponse`/`RoomDetailResponse`/`ReceivedRoomsResponse`/`MyRoomsResponse`/`MemoryResponse`/`MediaUploadResponse`. ID는 `@EncryptId`. `CreateXroomResponse.kt`는 `RoomResponse`(`{roomId}`)로 정리.

---

## 9. 단계별 PR 로드맵

| Phase | 범위 | 포함 엔드포인트 | 핵심 신규/수정 | 산출 DDL | 핵심 도메인 규칙 |
|-------|------|----------------|----------------|----------|------------------|
| **0** | 구모델 제거 + 토대 교체 | 없음(POST 시그니처만 body로) | XroomTheme/XroomValidator(→RoomValidator) 등 제거, XroomTable 스키마 교체, 매칭 hasXroom·탈퇴 cleaner/backup 컴파일 정합화, ObfuscationType/EntityType `MEMORY`/`MEDIA` 추가 | XROOMS(THEME 제거, template/title/finalMessage 추가) | 포트 시그니처 보존(exists/find), 컴파일 안정화 |
| **1** | Room 애그리거트 + 방 단위 API | 1,2,3,4(memories 빈배열),5 | Room/NewRoom/RoomTitle/FinalMessage/RoomTemplate/RoomValidator, Room*Repository, XroomCommand/QueryService, XroomQueryApi, request/response | (Phase 0 XROOMS) | 작성자 검증, 매칭 기반 received(§7), targetInfo당 active Room 1개 |
| **2** | Memory | 4(memories 채움),6,7,8 | EventDate/EventDatePrecision/EmotionTags/MemoryContent/Memory/Memories/NewMemory, Memory*Repository, MemoryCommand/QueryService, MemoryCommandApi, memoryCount 반영 | MEMORIES, MEMORY_EMOTION_TAGS | text⊕letter 상호배타, 시점 오름차순, emotionTags 저장/반환만 |
| **3** | Media/사진 | 9,10 | Media/NewMedia, Media*Repository, MemoryPhotoService, MemoryPhotoApi, FileStorage/ThumbnailGenerator 재활용, StorageDomainType.MEMORY | MEMORY_MEDIA | 기억당 active 1장(교체=기존 soft delete+새 active), 기억삭제 시 media 연쇄 soft delete, storageKey 메타만 |
| **4** (후속, 범위표시만) | 운영 전환 | — | 로컬 파일 cleanup 배치 job(soft delete된 media 파일 물리 삭제), FileStorage S3 어댑터 교체 | — | DB 메타 구조 유지, storage 구현만 교체 |

의존 순서: **0 → 1 → 2 → 3 → 4**. Phase 0이 깨지는 변경의 토대(매칭/탈퇴 컴파일 정합화 포함)이므로 반드시 선행.

---

## 10. 고려사항 / 확인 필요 사항

> 각 항목 추천안을 채택해 본문 설계는 구체화했고, 트레이드오프를 남긴다.

1. **수신자 판정 — 결정 불필요(claim 기구현으로 확정).** `findClaimedByTarget`가 이미 `targetId==me AND claimed==true`를 강제하므로, 수신자 = "등록자가 claim한 매칭에서 내가 target인 사람"으로 코드상 이미 정해져 있다. 별도 게이팅 결정/초대 엔티티 불필요 — 기존 쿼리 재사용만 하면 됨. (§7 참조)
2. **template/title 출처(추천: 서버 기본값)** — `title`=상수("기억의 방"), `template`=`CHAT_MEMORY`(wire `"chat_memory"`), `recipientName`=`TargetInfo.targetName` 파생. 트레이드오프: 생성 body에 없어 사용자 지정 불가 — 추후 body 필드 추가로 쉽게 확장.
3. **Room:targetInfo 1:1 — 옵션 B 확정(DB UNIQUE 제거 + 전부 soft delete + 앱 exists).** `TARGET_INFO_ID`엔 일반 INDEX만, DB UNIQUE는 두지 않는다. "targetInfo당 active Room 1개"는 `RoomQueryRepository.exists(targetInfoId)`(활성 행만 카운트)로 보장하고, soft-deleted 방은 무시되어 재생성이 자동 허용된다. 매칭 `hasXroom`(`exists(Set)`)도 활성 기준이라 의미 보존. 방·기억·사진 전부 soft delete로 통일, 로컬 파일은 cleanup job(Phase 4) 정리. → hard delete 안 함.
4. **emotionTags 저장방식 — 자식 테이블 확정.** 정규화 `MEMORY_EMOTION_TAGS`(MEMORY_ID 인덱스, 일급컬렉션 `EmotionTags`로 포장). 콤마조인 대안은 채택 안 함.
5. **사진 active 1장 보장** — partial/조건부 UNIQUE를 MariaDB가 미지원하므로 DB UNIQUE 불가, **애플리케이션(교체 시 기존 soft delete)**으로 보장. 동시 업로드 레이스는 트랜잭션+재조회로 완화. 확인 필요: 정적 서빙 방식(`file.upload.base-path` 하위 정적 리소스 핸들러 vs 스트리밍 컨트롤러). 추천: 정적 리소스 핸들러로 `photoUrl` 제공(dev), 운영은 S3 presigned로 교체.
6. **body 내 ID 복호화** — `@DecryptId`는 PathVariable/RequestParam에 적용되는 패턴. POST `/api/xrooms` body의 `targetInfoId`(암호화 String)는 Service에서 수동 복호화하거나 별도 처리 필요. 확인 필요: body ID 복호화 컨벤션(기존 사례 없음) — 대안은 targetInfoId를 RequestParam으로 유지.
7. **3단계 URL 중첩**(`/{roomId}/memories/{memoryId}/photo`) — code-implementation-rules §14 "2단계까지"를 초과. 사진을 메모리 종속 단일 리소스로 간주해 허용. 확인 필요: 별도 최상위 `/api/xrooms/media` 리소스로 평탄화할지(추천: 현행 유지, 직관적).
8. **FK 미사용**(프로젝트 정책) — ROOM_ID/MEMORY_ID 참조 무결성은 애플리케이션이 보장. 연쇄 soft delete(기억→media)는 Service에서 명시적 처리.
9. **성능** — `me`/`received` 목록의 memoryCount는 `countByRooms` 벌크(groupBy)로 N+1 방지. 상세의 photoUrl은 `findActiveByMemories` 벌크. exists는 `limit(1).any()`. 시점 정렬은 `(ROOM_ID, EVENT_DATE)` 복합 인덱스로 DB ORDER BY.
10. **운영 전환** — Media는 메타만 저장하므로 DB 구조 유지하고 `FileStorage` 구현만 LocalFileStorage→S3 어댑터로 교체 가능(헥사고날 포트 이점). 로컬 파일 즉시 삭제 안 하고 cleanup job 대상(Phase 4).

---

## 11. 외부 도메인 영향 정합화 (Phase 0 필수)

| 대상 파일 | 현재 호출 | 조정 |
|-----------|-----------|------|
| `matching/application/MatchingResultQueryService.kt` | `xroomQueryRepository.exists(Set<targetInfoId>)` → `toClaimerProfiles(...hasXroom)` | `RoomQueryRepository.exists(Set)` 시그니처 **보존** → 코드 무변경(타입명만 정합) |
| `withdrawal/domain/MemberDataCleaner.kt` | `cleanXroom` → `xroomCommandRepository.delete(ownerId)` | Room+하위 Memory/Media 일괄 정리로 확장(ownerId의 Room 조회 → memory/media softDelete → room softDelete). 포트 메서드 `softDelete(ownerId)` 유지 + 하위 정리 추가 |
| `withdrawal/domain/MemberWithdrawalBackupCollector.kt` | `xroomQueryRepository.find(member.id)` → `xrooms=` | `find(ownerId): List<Room>` 시그니처 보존. 백업 타입을 새 Room 모델로 교체(`MemberWithdrawalBackup.xrooms` 타입 변경). Memory/Media 백업 포함 여부는 확인 필요(추천: Phase 2/3에서 백업 항목 추가) |

---

## 12. 테스트 (구현 후 별도 — 항목만)

| 대상 | 테스트 내용 |
|------|-------------|
| `EventDate` | precision별 wire 변환/역변환, 정규화 DATE, 자릿수 검증 |
| `MemoryContent` | text⊕letter 상호배타(둘 다/둘 다 없음) |
| `EmotionTags` | 저장/반환, 빈 리스트 처리 |
| `FinalMessage`/`RoomTitle`/`RoomTemplate` | 기본값, 길이 검증, null/빈문자열 삭제 |
| `RoomValidator` | 소유권 + active Room 중복 |
| `Room`/`Memory` | validateOwnership, updateFinalMessage, update |
| `XroomCommand/QueryService` | create/updateFinalMessage, findMine/findReceived(매칭 기반)/findDetail 권한 |
| `MemoryCommandService` | add/update/remove(+media 연쇄 soft delete) |
| `MemoryPhotoService` | upload(교체=기존 soft delete+새 active), remove(active soft delete, 파일 미삭제) |
| Dao (db-core) | exists/find(EVENT_DATE ASC)/countByRooms(벌크)/softDelete |
| Api (web) | 10 엔드포인트 REST Docs, 작성자/수신자 권한 분기 |
