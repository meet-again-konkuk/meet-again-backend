# Plan: X룸 블록 저장 (Block Save)

> 작성일: 2026-04-18
> 최종 수정: 2026-04-18 (확정 사항 반영)

## 1. 개요

생성된 X룸에 콘텐츠 블록(PHOTO / SHORT_TEXT / LONG_TEXT / MUSIC / DDAY / VIDEO)을 저장한다.
각 블록 타입은 데이터 구조가 서로 다르므로 `DiscountPolicy` sealed 패턴을 따라 다형성으로 모델링하고,
DB는 CTI(Class Table Inheritance) — 부모(공통) + 타입별 자식 테이블 분리.

블록의 **표현 스타일**은 `XroomBlockItem` enum(액자/편지지/카드/프레임 등)이 결정하며,
아이템이 블록 타입별 한도(사진 max 수, 텍스트 max 길이)를 정의한다.

### 저장 흐름 결정 (A2: 블록 먼저 → 사진/영상 후속 업로드)

1. **블록 생성(단건)** — `POST /api/xrooms/{xroomId}/blocks` (JSON, 메타만)
2. **사진/영상 업로드** — `POST /api/xrooms/blocks/{blockId}/photos|videos` (multipart, 1회)
3. **사진/영상 교체(개별)** — `PUT /api/xrooms/blocks/{blockId}/photos|videos/{photoId}` (multipart, 1장)

> 파일 업로드와 메타 저장을 분리해 트랜잭션 단순화 + 클라이언트 UX 단계 분리.

### 도메인 분리

새 도메인을 만들지 않고 **기존 `xroom` 도메인의 하위 패키지(`xroom.domain.block`)** 로 추가한다.
- X룸 애그리거트의 일부 (X룸 없이 블록 단독 존재 불가)
- `feedback_no_new_domain.md` 정책 준수

---

## 2. API 설계

| Method | Endpoint | 용도 | Content-Type | 인증 |
|--------|----------|------|--------------|------|
| POST | `/api/xrooms/{xroomId}/blocks` | 블록 단건 생성 (메타) | application/json | 필요 |
| POST | `/api/xrooms/blocks/{blockId}/photos` | PHOTO 블록 사진 일괄 업로드 (1회) | multipart/form-data | 필요 |
| POST | `/api/xrooms/blocks/{blockId}/videos` | VIDEO 블록 영상 일괄 업로드 (1회) | multipart/form-data | 필요 |
| PUT | `/api/xrooms/blocks/{blockId}/photos/{photoId}` | 사진 1장 교체 | multipart/form-data | 필요 |
| PUT | `/api/xrooms/blocks/{blockId}/videos/{videoId}` | 영상 1개 교체 | multipart/form-data | 필요 |

`xroomId`는 `@PathVariable @DecryptId(ObfuscationType.XROOM)`, `blockId`/`photoId`/`videoId`도 동일 패턴으로 복호화.

### 2.1 블록 생성 (POST `/api/xrooms/{xroomId}/blocks`)

#### Request Body (Jackson polymorphic, type discriminator)

```json
{
  "type": "PHOTO",
  "item": "POLAROID_FRAME",
  "positionX": 120,
  "positionY": 80,
  "rotation": 0,
  "photoDate": "2024-03-21"
}
```

타입별 필드:

| type | 추가 필드 |
|------|-----------|
| PHOTO | `photoDate: LocalDate?` (사진 파일은 후속 업로드) |
| SHORT_TEXT | `text: String` (item이 정의한 maxLength 이내) |
| LONG_TEXT | `text: String` (item이 정의한 maxLength 이내) |
| MUSIC | `musicUrl: String`, `title: String`, `artist: String?` |
| DDAY | `anniversaryDate: LocalDate`, `label: String` |
| VIDEO | `description: String?` (영상 파일은 후속 업로드) |

- `type` discriminator: `@JsonTypeInfo(use = NAME, property = "type")` + `@JsonSubTypes`
- `item` 은 `XroomBlockItem` enum 값 그대로 (e.g. `POLAROID_FRAME`)
- `positionX`, `positionY`: **1~255 정수** (Validator로 범위 검증)
- `rotation`: 정수 (degree)

#### Response (201 Created)

```json
{ "blockId": 12345 }
```

- `blockId`는 `@EncryptId(XROOM_BLOCK)` 으로 암호화된 단일 식별자

### 2.2 사진/영상 업로드 (POST `/api/xrooms/blocks/{blockId}/photos|videos`)

#### Request (multipart/form-data)

- `@RequestPart("photos") photos: List<MultipartFile>` — 한 번에 N장
- 영상도 동일: `@RequestPart("videos") videos: List<MultipartFile>`

#### 검증

- `XroomBlockItem.maxPhotoCount` (또는 `maxVideoCount`)와 업로드 개수가 **정확히 일치**해야 함
- 1회만 호출 가능 (이미 업로드된 경우 409 Conflict)
- 블록 타입이 PHOTO(또는 VIDEO)가 아니면 400

#### Response (201 Created)

```json
{ "photoIds": [101, 102, 103] }   // 또는 videoIds
```

> 1차 범위에서는 `photoIds: List<Long>` 만 반환. URL은 별도 GET에서 조회.

### 2.3 사진/영상 교체 (PUT `/api/xrooms/blocks/{blockId}/photos|videos/{photoId}`)

#### Request (multipart/form-data, 1장)

- `@RequestPart("photo") photo: MultipartFile`

#### 동작

- 기존 `photoId`의 파일을 삭제하고 새 파일로 교체 (URL 갱신, `orderIndex` 유지)
- 블록 소유권/존재성 검증

#### Response (200 OK)

```json
{ "photoId": 101 }
```

---

## 3. 아키텍처

```
┌───────────────────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                                          │
│  XroomBlockCommandApi                                                     │
│   ├─ POST /api/xrooms/{xroomId}/blocks       (JSON)   → createBlock       │
│   ├─ POST /api/xrooms/blocks/{blockId}/photos (multipart) → uploadPhotos │
│   ├─ POST /api/xrooms/blocks/{blockId}/videos (multipart) → uploadVideos │
│   ├─ PUT  /api/xrooms/blocks/{blockId}/photos/{photoId}  → replacePhoto │
│   └─ PUT  /api/xrooms/blocks/{blockId}/videos/{videoId}  → replaceVideo │
└─────────────────────────────────┬─────────────────────────────────────────┘
                                  │ (port)
┌─────────────────────────────────▼─────────────────────────────────────────┐
│ domain/ma-domain-core (xroom.domain.block 하위 패키지)                    │
│  XroomBlockCommandService                                                 │
│   ├─ create(xroomId, email, newBlock): Long                              │
│   │     · xroom = xroomQueryRepository.findOne(xroomId)                  │
│   │     · xroom.validateOwnership(Email(email))                          │
│   │     · xroomBlockValidator.validate(newBlock)   // 좌표/호환성/길이   │
│   │     · xroomBlockCommandRepository.save(newBlock)                     │
│   ├─ uploadPhotos(blockId, email, files): List<Long>                     │
│   │     · block = xroomBlockQueryRepository.findOne(blockId)             │
│   │     · xroom.validateOwnership / block 타입 = PHOTO 검증              │
│   │     · block.validatePhotoCount(files.size)   // item.maxPhotoCount   │
│   │     · 이미 업로드되었는지 검증 (멱등 X, 충돌)                          │
│   │     · urls = photoUploader.upload(files)                              │
│   │     · xroomBlockPhotoCommandRepository.saveAll(blockId, Photos)      │
│   └─ replacePhoto(blockId, photoId, email, file): Long                    │
│         · 동일 검증 + photoUploader.upload(file)                          │
│         · xroomBlockPhotoCommandRepository.replace(photoId, newUrl)       │
│                                                                            │
│  도메인 모델 (sealed)                                                      │
│   XroomBlock (sealed)             — 조회/도메인 표현                       │
│    ├─ PhotoBlock(item, position, rotation, photoDate, photos: Photos)    │
│    ├─ ShortTextBlock(item, position, rotation, text)                     │
│    ├─ LongTextBlock(item, position, rotation, text)                      │
│    ├─ MusicBlock(item, position, rotation, musicUrl, title, artist)      │
│    ├─ DdayBlock(item, position, rotation, anniversaryDate, label)        │
│    └─ VideoBlock(item, position, rotation, description, videos: Videos)  │
│   NewXroomBlock (sealed)          — 저장 시점 표현 (id 없음)               │
│    ├─ NewPhotoBlock / NewShortTextBlock / NewLongTextBlock                │
│    ├─ NewMusicBlock / NewDdayBlock / NewVideoBlock                        │
│   Photos (일급 컬렉션, val data: List<Photo>)                             │
│   Videos (일급 컬렉션, val data: List<Video>)                             │
│   XroomBlockItem (enum, 메타 보유)                                         │
│   XroomBlockValidator (@Component)                                         │
│                                                                            │
│  Port                                                                      │
│   XroomBlockCommandRepository       (블록 메타 저장)                       │
│   XroomBlockQueryRepository         (블록 단건 조회)                       │
│   XroomBlockPhotoCommandRepository  (사진 saveAll, replace)                │
│   XroomBlockVideoCommandRepository  (영상 saveAll, replace)                │
│   PhotoUploader / VideoUploader     (파일 업로더 — 기존 인프라 재사용)     │
│   XroomQueryRepository              (findOne 추가)                         │
└─────────────────────────────────┬─────────────────────────────────────────┘
                                  │ (implements)
┌─────────────────────────────────▼─────────────────────────────────────────┐
│ infrastructure/storage/ma-db-core                                         │
│  XroomBlockTable             (공통: xroomId/blockType/item/pos/rotation) │
│  PhotoBlockTable             (photoDate)                                  │
│  ShortTextBlockTable         (text VARCHAR)                               │
│  LongTextBlockTable          (text TEXT)                                  │
│  MusicBlockTable             (musicUrl/title/artist)                      │
│  DdayBlockTable              (anniversaryDate/label)                      │
│  VideoBlockTable             (description)                                │
│  XroomBlockPhotoTable        (1:N: id, blockId, photoUrl, orderIndex)     │
│  XroomBlockVideoTable        (1:N: id, blockId, videoUrl, orderIndex)     │
│                                                                            │
│  XroomBlockEntity (sealed) + 타입별 6개 자식 Entity                       │
│  XroomBlockEntityFactory (interface) + 타입별 6개 @Component Factory      │
│                                                                            │
│  XroomBlockCommandDao / XroomBlockQueryDao                                │
│  XroomBlockPhotoCommandDao / XroomBlockVideoCommandDao                    │
│  *CoreRepository (포트 구현)                                               │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 변경 전략

### 4.1 도메인 모델 (sealed 계층)

| 클래스 | 유형 | 핵심 필드 / 행위 |
|--------|------|-----------------|
| `XroomBlockType` | enum | PHOTO, SHORT_TEXT, LONG_TEXT, MUSIC, DDAY, VIDEO |
| `XroomBlockItem` | enum | 메타: `compatibleType: XroomBlockType`, `maxPhotoCount: Int?`, `maxTextLength: Int?`, `maxVideoCount: Int?`. 예시: `POLAROID_FRAME(PHOTO, maxPhotoCount=1)`, `COLLAGE_3(PHOTO, maxPhotoCount=3)`, `GALLERY_WALL(PHOTO, maxPhotoCount=10)`, `PLAIN_CARD(SHORT_TEXT, maxTextLength=200)`, `LETTER_PAPER(LONG_TEXT, maxTextLength=2000)`, `LONG_LETTER(LONG_TEXT, maxTextLength=10000)`. `validatePhotoCount(n)`, `validateTextLength(t)`, `validateCompatibility(type)` 행위 보유 |
| `Position` | data class (선택) | `x: Int, y: Int` — init에서 1..255 검증. 1차에서는 `XroomBlockValidator`로 검증해도 됨 |
| `NewXroomBlock` | sealed class | `xroomId: Long`, `item: XroomBlockItem`, `positionX: Int`, `positionY: Int`, `rotation: Int`, abstract `type: XroomBlockType`. init에서 `item.validateCompatibility(type)` 호출 |
| `NewPhotoBlock` | NewXroomBlock 자식 | `photoDate: LocalDate?` |
| `NewShortTextBlock` | NewXroomBlock 자식 | `text: String`. init에서 `item.validateTextLength(text)` |
| `NewLongTextBlock` | NewXroomBlock 자식 | `text: String`. init에서 `item.validateTextLength(text)` |
| `NewMusicBlock` | NewXroomBlock 자식 | `musicUrl: String, title: String, artist: String?` |
| `NewDdayBlock` | NewXroomBlock 자식 | `anniversaryDate: LocalDate, label: String` |
| `NewVideoBlock` | NewXroomBlock 자식 | `description: String?` |
| `XroomBlock` | sealed class | 조회용. `validatePhotoCount(uploadCount)` 등 행위 보유 (item에 위임) |
| `Photo` | data class | `id: Long, photoUrl: String, orderIndex: Int` |
| `Photos` | 일급 컬렉션 (`val data: List<Photo>`) | `size`, `find(photoId)` 등. `Photos.from(blockId, urls)` 팩토리로 NewPhotos 생성용 |
| `Video` / `Videos` | Photo와 동일 패턴 | |

> **z-order**: 별도 컬럼 없음. 블록은 자유롭게 겹칠 수 있음 (확정).

### 4.2 검증 책임 분리

| 검증 항목 | 위치 | 비고 |
|-----------|------|------|
| 사용자 = X룸 소유자 | `Xroom.validateOwnership(Email)` (기존) | 도메인 객체 내부 |
| `positionX/Y ∈ 1..255` | `XroomBlockValidator.validate(newBlock)` | (또는 Position VO init) |
| `item ↔ blockType` 호환성 | `XroomBlockItem.validateCompatibility(type)` (init 시점) | 잘못된 조합은 객체 생성부터 차단 |
| TEXT 길이 ≤ `item.maxTextLength` | `XroomBlockItem.validateTextLength(text)` (init 시점) | |
| 사진 업로드 개수 = `item.maxPhotoCount` | `XroomBlock.validatePhotoCount(uploadCount)` | uploadPhotos 호출 시점 |
| 영상 업로드 개수 = `item.maxVideoCount` | `XroomBlock.validateVideoCount(uploadCount)` | uploadVideos 호출 시점 |
| 사진/영상 재업로드(1회 초과) | Service에서 `xroomBlockPhotoQueryRepository.exists(blockId)` → 기존 존재 시 409 | (또는 Validator) |
| 타입별 필수 필드 (URL non-blank 등) | 각 `NewXxxBlock` init 블록 | 도메인 객체 내부 |

`XroomBlockValidator`는 `@Component` (XroomValidator 패턴 동일), 좌표 등 도메인 객체 단독으로 판단 어려운 부분만 담당.

### 4.3 인프라 레이어 (CTI + 1:N 자산 테이블)

| 테이블 | 역할 | 주요 컬럼 (PK 외) |
|--------|------|-------------------|
| `XROOM_BLOCKS` (부모) | 공통 속성 + 디스크리미네이터 | XROOM_ID(BIGINT,NOT NULL), BLOCK_TYPE(VARCHAR(20),NOT NULL), ITEM(VARCHAR(40),NOT NULL), POSITION_X(TINYINT UNSIGNED,NOT NULL), POSITION_Y(TINYINT UNSIGNED,NOT NULL), ROTATION(INT,NOT NULL) + BaseTable |
| `XROOM_PHOTO_BLOCKS` | PHOTO | XROOM_BLOCK_ID(PK), PHOTO_DATE(DATE,NULL) |
| `XROOM_SHORT_TEXT_BLOCKS` | SHORT_TEXT | XROOM_BLOCK_ID(PK), TEXT(VARCHAR(500),NOT NULL) |
| `XROOM_LONG_TEXT_BLOCKS` | LONG_TEXT | XROOM_BLOCK_ID(PK), TEXT(TEXT,NOT NULL) |
| `XROOM_MUSIC_BLOCKS` | MUSIC | XROOM_BLOCK_ID(PK), MUSIC_URL(VARCHAR(512),NOT NULL), TITLE(VARCHAR(255),NOT NULL), ARTIST(VARCHAR(255),NULL) |
| `XROOM_DDAY_BLOCKS` | DDAY | XROOM_BLOCK_ID(PK), ANNIVERSARY_DATE(DATE,NOT NULL), LABEL(VARCHAR(100),NOT NULL) |
| `XROOM_VIDEO_BLOCKS` | VIDEO | XROOM_BLOCK_ID(PK), DESCRIPTION(VARCHAR(500),NULL) |
| `XROOM_BLOCK_PHOTOS` | 사진 1:N | ID(PK), BLOCK_ID(BIGINT,NOT NULL), PHOTO_URL(VARCHAR(512),NOT NULL), ORDER_INDEX(INT,NOT NULL) + BaseTable. INDEX(BLOCK_ID) |
| `XROOM_BLOCK_VIDEOS` | 영상 1:N | ID(PK), BLOCK_ID(BIGINT,NOT NULL), VIDEO_URL(VARCHAR(512),NOT NULL), ORDER_INDEX(INT,NOT NULL) + BaseTable. INDEX(BLOCK_ID) |

- **FK 미사용** (`feedback_no_fk.md`): 자식 테이블 PK == 부모 PK 값이지만 FK 제약 없음
- 자식(타입별) 테이블은 BaseTable 미상속 (감사 컬럼은 부모 + 자산 테이블만)
- 인덱스: `idx_xroom_block_xroom_id (XROOM_ID)`, `idx_xroom_block_photo_block_id (BLOCK_ID)`, `idx_xroom_block_video_block_id (BLOCK_ID)`
- TEXT 컬럼명은 MariaDB 예약어 가능성 → 백틱(`` `TEXT` ``) 사용 또는 `BODY`로 변경

### 4.4 DAO 저장 전략

`XroomBlockCommandDao.save(block: NewXroomBlock): Long`
1. `XroomBlockTable.insert` → 부모 PK 획득
2. `when (block)` sealed 분기 → 타입별 자식 테이블 insert (id = 부모 PK 값)
3. 부모 PK 반환

`XroomBlockPhotoCommandDao.saveAll(blockId, photoUrls: List<String>): List<Long>`
- `XroomBlockPhotoTable.batchInsert(photoUrls.withIndex())` → orderIndex = index, 생성 ID 리스트 반환

`XroomBlockPhotoCommandDao.replace(photoId, newUrl)`
- `XroomBlockPhotoTable.update({ id eq photoId }) { it[PHOTO_URL] = newUrl }`

영상 DAO도 동일 패턴.

### 4.5 DAO 조회 전략

`XroomBlockQueryDao.findOne(blockId): XroomBlockEntity`
- 부모 + LEFT JOIN 자식 6개 (BLOCK_TYPE으로 어느 자식이 채워지는지 구분)
- 또는 부모 BLOCK_TYPE 조회 → 해당 타입 Factory가 자식 단일 SELECT (2쿼리, 단순)

`XroomBlockPhotoQueryDao.exists(blockId): Boolean`
- `limit(1).any()` (count 사용 금지, code-implementation-rules §18)

### 4.6 Controller 레이어 (polymorphic Request)

| 항목 | 처리 |
|------|------|
| Request 다형성 | `CreateXroomBlockRequest` sealed class + `@JsonTypeInfo(use=NAME, include=PROPERTY, property="type")` + `@JsonSubTypes` |
| 자식 Request | `CreatePhotoBlockRequest` / `ShortText` / `LongText` / `Music` / `Dday` / `Video` (각 `@JsonTypeName`) |
| 도메인 변환 | 각 Request에 `toCommand(xroomId): NewXroomBlock`. **Request 단계에서 도메인 VO 직접 생성 금지** — Command/도메인 모델이 String/Int를 받아 내부에서 변환 (`feedback_domain_object_in_domain.md`) |
| Path 복호화 | `@DecryptId(XROOM)`, `@DecryptId(XROOM_BLOCK)`, `@DecryptId(XROOM_BLOCK_PHOTO)`, `@DecryptId(XROOM_BLOCK_VIDEO)` |
| Response | `CreateXroomBlockResponse(@EncryptId(XROOM_BLOCK) blockId: Long)`, `UploadPhotosResponse(@EncryptId(XROOM_BLOCK_PHOTO) photoIds: List<Long>)` |
| ObfuscationType / EntityType | `XROOM_BLOCK`, `XROOM_BLOCK_PHOTO`, `XROOM_BLOCK_VIDEO` 추가 |
| multipart 파라미터 | `@RequestPart("photos") List<MultipartFile>`, `@RequestPart("photo") MultipartFile` (단건 교체) |

### 4.7 Service 흐름

`XroomBlockCommandService.create(xroomId, email, newBlock): Long`
1. `xroom = xroomQueryRepository.findOne(xroomId)`
2. `xroom.validateOwnership(Email(email))`
3. `xroomBlockValidator.validate(newBlock)` — 좌표/추가 검증 (item↔type 호환·길이는 도메인 init에서 이미 처리)
4. `xroomBlockCommandRepository.save(newBlock)` → blockId 반환

`XroomBlockCommandService.uploadPhotos(blockId, email, files): List<Long>`
1. `block = xroomBlockQueryRepository.findOne(blockId)` (Xroom 정보 포함 또는 별도 조회)
2. 소유권 검증 (Xroom)
3. `block.validatePhotoCount(files.size)` (item.maxPhotoCount)
4. 이미 업로드되었는지 검증 (재업로드 차단)
5. `urls = photoUploader.upload(files)` (포트, 인프라가 구현)
6. `xroomBlockPhotoCommandRepository.saveAll(blockId, urls)` → photoIds 반환

`XroomBlockCommandService.replacePhoto(blockId, photoId, email, file): Long`
1. block + photo 조회 / 소유권 검증
2. `newUrl = photoUploader.upload(file)`
3. `xroomBlockPhotoCommandRepository.replace(photoId, newUrl)` → photoId 반환

영상은 video 버전으로 동일 패턴.

### 4.8 기존 코드 변경

| 파일 | 변경 내용 |
|------|-----------|
| `XroomQueryRepository` (port) | `fun findOne(xroomId: Long): Xroom` 추가 (현재는 `exists`만) |
| `XroomQueryCoreRepository` | `findOne` 구현 추가 |
| `XroomQueryDao` | `findOne(xroomId): XroomEntity` 추가 (Repository에서 toDomain) |
| `ObfuscationType` | `XROOM_BLOCK`, `XROOM_BLOCK_PHOTO`, `XROOM_BLOCK_VIDEO` 추가 |
| `EntityType` | 동일 3종 추가 |
| `ddl.sql` | XROOM_BLOCKS + 타입별 6개 + XROOM_BLOCK_PHOTOS + XROOM_BLOCK_VIDEOS = 9개 테이블 추가 (FK 없음) |

---

## 5. 변경 파일 목록

### Phase 1: DDL

| # | 파일 | 내용 |
|---|------|------|
| 1 | `infrastructure/.../script/ddl.sql` | XROOM_BLOCKS(부모) + 타입별 6개(PHOTO/SHORT_TEXT/LONG_TEXT/MUSIC/DDAY/VIDEO) + XROOM_BLOCK_PHOTOS + XROOM_BLOCK_VIDEOS = 9개 테이블, FK 없음, INDEX(XROOM_ID/BLOCK_ID) |

### Phase 2: Domain Model

| # | 파일 | 내용 |
|---|------|------|
| 2 | `domain/.../xroom/domain/block/XroomBlockType.kt` | enum (PHOTO, SHORT_TEXT, LONG_TEXT, MUSIC, DDAY, VIDEO) |
| 3 | `domain/.../xroom/domain/block/XroomBlockItem.kt` | enum + 메타(`compatibleType`, `maxPhotoCount`, `maxTextLength`, `maxVideoCount`) + 행위(`validateCompatibility`, `validatePhotoCount`, `validateVideoCount`, `validateTextLength`) |
| 4 | `domain/.../xroom/domain/block/NewXroomBlock.kt` | sealed class, 공통 필드 + abstract `type`. init에서 item↔type 호환성 검증 |
| 5 | `domain/.../xroom/domain/block/NewPhotoBlock.kt` | photoDate |
| 6 | `domain/.../xroom/domain/block/NewShortTextBlock.kt` | text (init에서 길이 검증) |
| 7 | `domain/.../xroom/domain/block/NewLongTextBlock.kt` | text (init에서 길이 검증) |
| 8 | `domain/.../xroom/domain/block/NewMusicBlock.kt` | musicUrl/title/artist |
| 9 | `domain/.../xroom/domain/block/NewDdayBlock.kt` | anniversaryDate/label |
| 10 | `domain/.../xroom/domain/block/NewVideoBlock.kt` | description |
| 11 | `domain/.../xroom/domain/block/XroomBlock.kt` | sealed class (조회용) + 6개 타입별 자식 + `validatePhotoCount(n)` 등 행위 |
| 12 | `domain/.../xroom/domain/block/Photo.kt`, `Photos.kt` | `Photo(id, photoUrl, orderIndex)` + 일급 컬렉션 |
| 13 | `domain/.../xroom/domain/block/Video.kt`, `Videos.kt` | 동일 패턴 |
| 14 | `domain/.../xroom/domain/block/XroomBlockValidator.kt` | `@Component`, `validate(newBlock)` — 좌표 검증 등 |

### Phase 3: Domain Port + 기존 수정

| # | 파일 | 내용 |
|---|------|------|
| 15 | `domain/.../xroom/domain/port/XroomBlockCommandRepository.kt` | `fun save(newBlock: NewXroomBlock): Long` |
| 16 | `domain/.../xroom/domain/port/XroomBlockQueryRepository.kt` | `fun findOne(blockId: Long): XroomBlock` |
| 17 | `domain/.../xroom/domain/port/XroomBlockPhotoCommandRepository.kt` | `fun saveAll(blockId: Long, urls: List<String>): List<Long>`, `fun replace(photoId: Long, newUrl: String)` |
| 18 | `domain/.../xroom/domain/port/XroomBlockPhotoQueryRepository.kt` | `fun exists(blockId: Long): Boolean`, `fun findOne(photoId: Long): Photo` |
| 19 | `domain/.../xroom/domain/port/XroomBlockVideoCommandRepository.kt` | 영상 동일 패턴 |
| 20 | `domain/.../xroom/domain/port/XroomBlockVideoQueryRepository.kt` | 영상 동일 패턴 |
| 21 | `domain/.../xroom/domain/port/PhotoUploader.kt` | `fun upload(files: List<MultipartFile>): List<String>`, `fun upload(file: MultipartFile): String` (또는 기존 업로더 재사용 — 코드 탐색 필요) |
| 22 | `domain/.../xroom/domain/port/VideoUploader.kt` | 영상 업로더 (또는 기존 재사용) |
| 23 | `domain/.../xroom/domain/port/XroomQueryRepository.kt` | `fun findOne(xroomId: Long): Xroom` 추가 |
| 24 | `domain/.../common/domain/id/ObfuscationType.kt` | `XROOM_BLOCK`, `XROOM_BLOCK_PHOTO`, `XROOM_BLOCK_VIDEO` 추가 |
| 25 | `domain/.../exception/EntityType.kt` | 동일 3종 추가 |

### Phase 4: Application Service

| # | 파일 | 내용 |
|---|------|------|
| 26 | `domain/.../xroom/application/XroomBlockCommandService.kt` | `@Service @Transactional`. `create(xroomId, email, newBlock): Long`, `uploadPhotos(blockId, email, files): List<Long>`, `uploadVideos(...)`, `replacePhoto(blockId, photoId, email, file): Long`, `replaceVideo(...)` |

### Phase 5: Infrastructure - Tables

| # | 파일 | 내용 |
|---|------|------|
| 27 | `infrastructure/.../xroom/entity/table/XroomBlockTable.kt` | BaseTable, XROOM_ID/BLOCK_TYPE/ITEM/POSITION_X(TINYINT)/POSITION_Y/ROTATION |
| 28 | `infrastructure/.../xroom/entity/table/PhotoBlockTable.kt` | LongIdTable("XROOM_PHOTO_BLOCKS","XROOM_BLOCK_ID"), PHOTO_DATE |
| 29 | `infrastructure/.../xroom/entity/table/ShortTextBlockTable.kt` | TEXT VARCHAR(500) |
| 30 | `infrastructure/.../xroom/entity/table/LongTextBlockTable.kt` | TEXT TEXT |
| 31 | `infrastructure/.../xroom/entity/table/MusicBlockTable.kt` | MUSIC_URL/TITLE/ARTIST |
| 32 | `infrastructure/.../xroom/entity/table/DdayBlockTable.kt` | ANNIVERSARY_DATE/LABEL |
| 33 | `infrastructure/.../xroom/entity/table/VideoBlockTable.kt` | DESCRIPTION |
| 34 | `infrastructure/.../xroom/entity/table/XroomBlockPhotoTable.kt` | BaseTable, BLOCK_ID/PHOTO_URL/ORDER_INDEX |
| 35 | `infrastructure/.../xroom/entity/table/XroomBlockVideoTable.kt` | BaseTable, BLOCK_ID/VIDEO_URL/ORDER_INDEX |

### Phase 6: Infrastructure - Entities & Factories

| # | 파일 | 내용 |
|---|------|------|
| 36 | `infrastructure/.../xroom/entity/XroomBlockEntity.kt` | sealed class — 공통 + abstract `toDomain(): XroomBlock` |
| 37 | `infrastructure/.../xroom/entity/PhotoBlockEntity.kt` | toDomain → PhotoBlock |
| 38 | `infrastructure/.../xroom/entity/ShortTextBlockEntity.kt` | |
| 39 | `infrastructure/.../xroom/entity/LongTextBlockEntity.kt` | |
| 40 | `infrastructure/.../xroom/entity/MusicBlockEntity.kt` | |
| 41 | `infrastructure/.../xroom/entity/DdayBlockEntity.kt` | |
| 42 | `infrastructure/.../xroom/entity/VideoBlockEntity.kt` | |
| 43 | `infrastructure/.../xroom/entity/XroomBlockPhotoEntity.kt` | toDomain → Photo |
| 44 | `infrastructure/.../xroom/entity/XroomBlockVideoEntity.kt` | toDomain → Video |
| 45 | `infrastructure/.../xroom/dao/XroomBlockEntityFactory.kt` | interface — `type`, `childTable`, `createFrom(parentRow, childRow)` |
| 46 | `infrastructure/.../xroom/dao/PhotoBlockEntityFactory.kt` ~ `VideoBlockEntityFactory.kt` | `@Component` 6개 |

### Phase 7: Infrastructure - DAO & Repository

| # | 파일 | 내용 |
|---|------|------|
| 47 | `infrastructure/.../xroom/dao/XroomBlockCommandDao.kt` | `save(block): Long` — 부모 insert 후 sealed when 분기 자식 insert |
| 48 | `infrastructure/.../xroom/dao/XroomBlockQueryDao.kt` | `findOne(blockId): XroomBlockEntity` — 부모+자식 조회, Factory로 매핑 |
| 49 | `infrastructure/.../xroom/dao/XroomBlockPhotoCommandDao.kt` | `saveAll(blockId, urls): List<Long>` (batchInsert), `replace(photoId, url)` |
| 50 | `infrastructure/.../xroom/dao/XroomBlockPhotoQueryDao.kt` | `exists(blockId)` (limit(1).any()), `findOne(photoId)` |
| 51 | `infrastructure/.../xroom/dao/XroomBlockVideoCommandDao.kt` | 영상 동일 |
| 52 | `infrastructure/.../xroom/dao/XroomBlockVideoQueryDao.kt` | 영상 동일 |
| 53 | `infrastructure/.../xroom/repository/XroomBlockCommandCoreRepository.kt` | 포트 구현, DAO 위임 |
| 54 | `infrastructure/.../xroom/repository/XroomBlockQueryCoreRepository.kt` | 포트 구현, entity.toDomain() 변환 |
| 55 | `infrastructure/.../xroom/repository/XroomBlockPhotoCommandCoreRepository.kt` | 포트 구현 |
| 56 | `infrastructure/.../xroom/repository/XroomBlockPhotoQueryCoreRepository.kt` | 포트 구현 |
| 57 | `infrastructure/.../xroom/repository/XroomBlockVideoCommandCoreRepository.kt` | 포트 구현 |
| 58 | `infrastructure/.../xroom/repository/XroomBlockVideoQueryCoreRepository.kt` | 포트 구현 |
| 59 | `infrastructure/.../xroom/dao/XroomQueryDao.kt` | `findOne(xroomId): XroomEntity` 추가 |
| 60 | `infrastructure/.../xroom/repository/XroomQueryCoreRepository.kt` | `findOne(xroomId): Xroom` 구현 추가 |
| 61 | `infrastructure/.../upload/PhotoUploaderAdapter.kt` (or 기존) | `PhotoUploader` 포트 구현 — 기존 파일 업로드 인프라가 있다면 재사용 (탐색 필요) |
| 62 | `infrastructure/.../upload/VideoUploaderAdapter.kt` | `VideoUploader` 포트 구현 |

### Phase 8: Boot - API Layer

| # | 파일 | 내용 |
|---|------|------|
| 63 | `boot/.../xroom/api/request/CreateXroomBlockRequest.kt` | sealed class + `@JsonTypeInfo`/`@JsonSubTypes`. 자식 6개. 각 자식에 `toCommand(xroomId): NewXroomBlock` |
| 64 | `boot/.../xroom/api/response/CreateXroomBlockResponse.kt` | `@EncryptId(XROOM_BLOCK) blockId: Long` |
| 65 | `boot/.../xroom/api/response/UploadPhotosResponse.kt` | `@EncryptId(XROOM_BLOCK_PHOTO) photoIds: List<Long>` |
| 66 | `boot/.../xroom/api/response/UploadVideosResponse.kt` | 영상 동일 |
| 67 | `boot/.../xroom/api/response/ReplacePhotoResponse.kt` | `@EncryptId(XROOM_BLOCK_PHOTO) photoId: Long` |
| 68 | `boot/.../xroom/api/response/ReplaceVideoResponse.kt` | 영상 동일 |
| 69 | `boot/.../xroom/api/XroomBlockCommandApi.kt` | `@RestController`. 5개 엔드포인트 (블록 생성, 사진/영상 업로드, 사진/영상 교체) |
| 70 | `boot/.../test/.../vocabulary/XroomBlockVocabulary.kt` | 블록 필드 정의 함수 (`type()`, `item()`, `positionX()`, `positionY()`, `rotation()`, `photoDate()`, `text()`, `musicUrl()`, `anniversaryDate()`, `label()`, `description()`, `blockId()`, `photoIds()`, `videoIds()`, `photoId()`, `videoId()`) |

### Phase 9: Test

| # | 파일 | 내용 |
|---|------|------|
| 71 | `domain/.../xroom/fixture/XroomBlockFixture.kt` | NewPhotoBlock 등 6종 팩토리 메서드 |
| 72 | `domain/.../test/.../block/XroomBlockItemTest.kt` | 호환성/길이/사진수/영상수 검증 |
| 73 | `domain/.../test/.../block/NewXroomBlockTest.kt` | init 시점 호환성/길이 검증 |
| 74 | `domain/.../test/.../block/XroomBlockValidatorTest.kt` | 좌표 1..255 외 거부 |
| 75 | `domain/.../test/.../application/XroomBlockCommandServiceTest.kt` | create / uploadPhotos / uploadVideos / replacePhoto / replaceVideo 흐름 |
| 76 | `infrastructure/.../test/.../dao/XroomBlockCommandDaoTest.kt` | 부모+자식 insert 정합성 |
| 77 | `infrastructure/.../test/.../dao/XroomBlockQueryDaoTest.kt` | findOne 매핑 정확성 |
| 78 | `infrastructure/.../test/.../dao/XroomBlockPhotoCommandDaoTest.kt` | saveAll batchInsert + replace |
| 79 | `boot/.../test/.../api/XroomBlockCommandApiTest.kt` | REST Docs (다형성 Request, multipart 업로드, 교체) |

---

## 6. 고려사항

- **Sealed 패턴 일관성**: `DiscountPolicy` (sealed) + `DiscountPolicyEntity` (sealed) + `DiscountPolicyEntityFactory` 구조를 그대로 따른다. 신규 블록 타입 추가 시 (1) 도메인 sealed 자식 (2) 엔티티 sealed 자식 (3) Table (4) Factory만 추가하면 됨.
- **Jackson polymorphic deserialization**: `@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")` + `@JsonSubTypes` 으로 `type` 필드 분기. Spring Boot 기본 ObjectMapper에서 동작.
- **`XroomBlockItem` 메타 설계**: enum이 메타(maxPhotoCount, maxTextLength, maxVideoCount, compatibleType)를 보유. 새 아이템 추가 시 enum 한 줄 추가로 끝. OCP 만족.
- **TINYINT UNSIGNED 사용**: `positionX/Y` 1~255 범위 → TINYINT UNSIGNED(0~255). DB 레벨 1차 가드 + 도메인 레벨 검증 이중화. Exposed에서는 `ubyte()` 사용.
- **TEXT 컬럼명 충돌**: MariaDB 예약어 → 백틱 또는 `BODY`로 변경. Exposed의 `varchar("text", 500)`은 SQL 생성 시 백틱 자동 처리되는지 확인 필요.
- **사진 1:N 정렬**: `ORDER_INDEX` 컬럼으로 보장. `saveAll`에서 입력 순서대로 0,1,2,... 부여.
- **재업로드 차단**: 1차 범위에서는 1회만 허용. `XroomBlockPhotoQueryRepository.exists(blockId)` → 존재 시 409 Conflict.
- **트랜잭션**: 부모/자식 INSERT는 단일 트랜잭션. `@Transactional`은 Service에. 사진 업로드는 (S3 업로드 + DB 저장) 분리되므로 보상 트랜잭션이나 미완 정리는 1차 범위 외.
- **파일 업로더**: 기존 회원 프로필/아바타 업로드 코드가 있다면 재사용. 없다면 `domain/.../upload/PhotoUploader` 포트 + `infrastructure/.../upload` 어댑터 신설.
- **검증 위치 정리**: `XroomBlockItem` (메타+호환성/길이) / `Xroom.validateOwnership` (소유권) / `NewXxxBlock.init` (자기 자신) / `XroomBlockValidator` (좌표 등 좌표는 도메인 단독으로 판단 어려운 부분만) / `XroomBlock.validatePhotoCount` (업로드 수). Service는 흐름만.
- **도메인 객체 변환은 도메인 모듈 내부에서**: Request에서 `Email()` 등 VO 직접 생성 금지. Request → Command → 도메인 모델 흐름에서 String/Int를 그대로 넘기고 도메인 init에서 변환 (`feedback_domain_object_in_domain.md`).
- **API 테스트**: `BaseApiTest`에 `@WithAuthMember` 기본 포함. Api 테스트 클래스에 중복 선언 금지 (`feedback_no_duplicate_with_auth_member.md`).
- **REST Docs**: 다형성 Request 표현은 type별로 별도 snippet 필요 (PhotoBlock 케이스, ShortTextBlock 케이스 등).

---

## 7. 확정 사항

본 구현 계획은 다음 결정사항을 반영한다.

### 7.1 좌표/순서

- `positionX`, `positionY`: **1~255 정수**, `XroomBlockValidator`에서 범위 검증
- **z-order 별도 컬럼/처리 없음** — 블록은 자유롭게 겹칠 수 있음
- `rotation`: 정수 (degree)

### 7.2 응답

- 블록 저장 응답: `{ blockId: Long }` (단일 식별자)
- 사진 업로드 응답: `{ photoIds: List<Long> }`
- 사진 교체 응답: `{ photoId: Long }`
- 영상도 동일 패턴

### 7.3 블록 타입 (sealed)

- `PHOTO` — 사진 + `photoDate` 묶음, **caption 없음**
- `SHORT_TEXT` — 단문 텍스트
- `LONG_TEXT` — 장문 편지 텍스트
- `MUSIC` — 음원 URL + 메타
- `DDAY` — 기념일 + 라벨
- `VIDEO` — 영상 + 설명

### 7.4 꾸미기 아이템 (`XroomBlockItem` enum)

- 아이템이 콘텐츠 표현 스타일을 결정 (액자/편지지/카드/프레임 등)
- 아이템이 블록 타입별 한도를 정의:
  - PHOTO: max 사진 수 (예: `POLAROID_FRAME=1`, `COLLAGE_3=3`, `GALLERY_WALL=10`)
  - TEXT: max 텍스트 길이 (예: `PLAIN_CARD=200`, `LETTER_PAPER=2000`, `LONG_LETTER=10000`)
- enum이 메타(max 사진/영상 수, max 텍스트 길이, 호환 BlockType)를 보유
- 아이템 ↔ 블록 타입 호환성 검증 (`item.validateCompatibility(type)`) — `NewXroomBlock` init에서 호출

### 7.5 API 흐름 (A2: 블록 먼저 → 사진/영상 후속 업로드)

1. **블록 생성(단건)**: `POST /api/xrooms/{xroomId}/blocks` (JSON, 메타만)
2. **사진/영상 업로드(1회)**: `POST /api/xrooms/blocks/{blockId}/photos|videos` (multipart, item이 정의한 max와 정확히 일치)
3. **사진/영상 교체(개별)**: `PUT /api/xrooms/blocks/{blockId}/photos|videos/{photoId}` (multipart, 1장)

### 7.6 도메인/DB

- 부모 테이블 `XROOM_BLOCKS` + 타입별 자식 6개 + 자산 1:N 2개 = **9개 신규 테이블**
- **FK 사용 안 함, INDEX만**
- 도메인: `XroomBlock` sealed + `NewXroomBlock` sealed + `Photos`/`Videos` 일급 컬렉션 + `XroomBlockItem` enum + `XroomBlockValidator`

---

## 8. 1차 범위 외 (별도 plan으로 분리)

- 사진/영상 추가/삭제 (장수 변경)
- 업로드 미완료 블록 자동 정리 (보상 트랜잭션 / 배치)
- 멱등성 처리 (중복 호출 방지 — 클라이언트 발급 UUID 등)
- 블록 수정 (위치/회전/아이템 변경 등 메타 PATCH)
- 블록 삭제 (soft / hard)
- 블록 목록 조회 (`GET /api/xrooms/{xroomId}/blocks`)
- 동시성 제어 (수량 race condition)

---

## 9. 검증 항목

- [ ] DDL 9개 테이블 정상 생성 (FK 없음, 인덱스 정상, TEXT 컬럼명 충돌 해결)
- [ ] `XroomBlockItem` 호환성/길이/사진수/영상수 검증 단위 테스트 통과
- [ ] `NewXroomBlock` init에서 item↔type 비호환 케이스 거부 (LONG_TEXT 아이템에 PHOTO 블록 등)
- [ ] `XroomBlockValidator` 좌표 0/256 거부, 1/255 허용
- [ ] `XroomBlockCommandService.create` 소유권 위반 → `AccessDeniedException`
- [ ] `uploadPhotos` item.maxPhotoCount와 다른 개수 → 거부 / 정확히 일치 → 성공
- [ ] `uploadPhotos` 재호출 → 409 Conflict
- [ ] `replacePhoto` 다른 블록의 photoId → 거부 / 정상 케이스 URL 갱신
- [ ] `XroomBlockCommandDao.save` 부모/자식 INSERT 정합성 (sealed 분기 누락 없음)
- [ ] `XroomBlockPhotoCommandDao.saveAll` ORDER_INDEX 0..N-1 정확
- [ ] REST Docs: 다형성 Request 6타입 + multipart 업로드 + 교체 snippet 생성
- [ ] `./gradlew build` 성공
- [ ] 기존 `XroomCommandApiTest`, `XroomCommandServiceTest` 회귀 통과

---

## 10. 후속 작업 (참고)

- `GET /api/xrooms/{xroomId}/blocks` — 블록 목록 조회 (XroomBlock sealed 활용)
- `PATCH /api/xrooms/blocks/{blockId}` — 블록 메타 수정 (위치/회전/아이템 교체)
- `DELETE /api/xrooms/blocks/{blockId}` — 블록 삭제
- 사진/영상 추가/삭제 API
- 업로드 후 미사용 자산 정리 배치
- 동시성 제어 (수량 한도 race condition)

---

## 📋 스킬 적용 체크리스트

### plan-writing 스킬
- [x] SKILL.md 파일을 Read로 읽었는가
- [x] 코드 스니펫 없이 시그니처/설명 수준으로 작성했는가 (JSON 예시는 API 명세 표현 목적)
- [x] 변경 전략을 테이블로 정리했는가
- [x] Phase별 변경 파일 목록을 한 줄 요약으로 작성했는가
- [x] `docs/plan/{YYYYMM}/` 디렉토리에 저장했는가 (`docs/plan/202604/`)
- [x] 고려사항(성능, FK, 예약어, 트랜잭션 등)을 포함했는가

### code-implementation-rules 스킬
- [x] SKILL.md 파일을 Read로 읽었는가
- [x] Service는 조합만 담당, 검증은 Validator/도메인 객체로 분리했는가
- [x] 도메인 객체에 행위 부여 (item.validateCompatibility, block.validatePhotoCount)
- [x] 일급 컬렉션 사용 (Photos/Videos, `val data`)
- [x] 포트 인터페이스가 도메인 객체 사용
- [x] 단건 조회는 non-null 반환 (findOne)
- [x] FK 미사용 (feedback_no_fk.md)
- [x] exists는 limit(1).any() 사용 명시
- [x] 도메인 객체 변환은 도메인 모듈 내부 (feedback_domain_object_in_domain.md)
