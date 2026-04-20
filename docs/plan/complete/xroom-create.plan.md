# Plan: X룸 생성

> 작성일: 2026-04-16

## 1. 개요

지정한 Target-Info 대상으로 X룸을 생성하는 기능을 구현한다. X룸은 과거 연인과의 추억을 테마 공간에 블록 단위로 배치하여 꾸미는 기능이며, 먼저 생성 후 추후 블록들을 업데이트하면서 완성해가는 형태이다. 테마는 우선 기본값(CORK_BOARD)으로 고정하되, 확장 가능하도록 enum으로 설계한다.

### 도메인 분리 판단

X룸은 **독립 도메인(xroom)**으로 분리한다.
- 매칭과 별개의 생명주기를 가짐 (생성, 테마 변경, 블록 관리, 공유, 삭제)
- 자체 애그리거트 루트(Xroom)와 하위 엔티티(Block)를 포함
- 향후 테마, 블록, 공유 등 기능이 확장될 예정
- 매칭 도메인에 넣으면 매칭 도메인이 비대해짐

---

## 2. API 설계

api-todo.md 기준 `POST /api/xrooms` 항목에 해당한다.

| Method | Endpoint | 용도 | 인증 |
|--------|----------|------|------|
| POST | `/api/xrooms` | X룸 생성 | 필요 |

**Request Body**: 없음 (targetInfoId를 쿼리 파라미터 또는 body로 전달)

요청 시 targetInfoId를 받아 해당 Target-Info에 대한 X룸을 생성한다. 테마는 현재 기본값(CORK_BOARD)으로 고정되므로 Request Body에 포함하지 않되, 향후 테마 선택 기능 추가 시 Request Body에 theme 필드를 추가하면 된다.

**Request Body**:
```json
{
  "targetInfoId": "암호화된 ID"
}
```

**Response** (201 Created):
```json
{
  "xroomId": "암호화된 ID"
}
```

---

## 3. 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                            │
│  XroomCommandApi                                            │
│    └── POST /api/xrooms → commandService.create()           │
└────────────────────────────┬────────────────────────────────┘
                             │ (port)
┌────────────────────────────▼────────────────────────────────┐
│ domain/ma-domain-core                                       │
│  XroomCommandService                                        │
│    └── create(targetInfoId, email) → xroomId                │
│  XroomCommandRepository (port)                              │
│    + save(newXroom: NewXroom): Long                          │
│  TargetInfoQueryRepository (기존 port)                      │
│    + findOne(targetInfoId) — 소유권 검증 + 존재 확인용       │
│  XroomQueryRepository (port)                                │
│    + existsByTargetInfoId(targetInfoId: Long): Boolean       │
└────────────────────────────┬────────────────────────────────┘
                             │ (implements)
┌────────────────────────────▼────────────────────────────────┐
│ infrastructure/storage/ma-db-core                           │
│  XroomTable: XROOMS 테이블 정의                              │
│  XroomEntity: toDomain() / from()                           │
│  XroomCommandDao: save()                                    │
│  XroomQueryDao: existsByTargetInfoId()                      │
│  XroomCommandCoreRepository: 포트 구현                       │
│  XroomQueryCoreRepository: 포트 구현                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 변경 전략

| 레이어 | 내용 | 비고 |
|--------|------|------|
| DDL | XROOMS 테이블 신규 생성 | OWNER_EMAIL, TARGET_INFO_ID, THEME + BaseTable 공통 컬럼 |
| Domain Model | Xroom, NewXroom, XroomTheme 신규 | xroom 도메인 패키지 신규 생성 |
| Port | XroomCommandRepository, XroomQueryRepository 신규 | 기존 TargetInfoQueryRepository 재활용 (소유권 검증) |
| Service | XroomCommandService 신규 | TargetInfo 존재/소유권 검증 → 중복 생성 방지 → save |
| Infrastructure | Table, Entity, DAO, Repository 신규 | community 패턴과 동일한 구조 |
| Boot | XroomCommandApi, Request/Response 신규 | ObfuscationType에 XROOM 추가 |

---

## 5. 상세 설계

### 5.1 DDL - XROOMS 테이블

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`
**변경 유형**: 수정 (테이블 추가)

- XROOM_ID (PK, BIGINT AUTO_INCREMENT)
- OWNER_EMAIL (VARCHAR(255), NOT NULL) — X룸 소유자
- TARGET_INFO_ID (BIGINT, NOT NULL) — 대상 Target-Info ID
- THEME (VARCHAR(32), NOT NULL, DEFAULT 'CORK_BOARD') — 테마
- BaseTable 공통 컬럼들 (CREATED_DATE, CREATED_BY, ... DELETED 등)
- INDEX: `idx_xroom_owner_email (OWNER_EMAIL)`, `UNIQUE INDEX idx_xroom_target_info_id (TARGET_INFO_ID)` — 동일 Target-Info에 대해 X룸 중복 생성 방지

### 5.2 Domain - XroomTheme (enum)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/xroom/domain/XroomTheme.kt`
**변경 유형**: 신규

- enum values: `CORK_BOARD`, `STRING_LIGHT`, `DREAMY_BUBBLE`
- api-todo.md에 정의된 3가지 테마에 대응
- 현재는 CORK_BOARD만 사용하되, enum으로 확장 가능하게 설계

### 5.3 Domain - NewXroom

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/xroom/domain/NewXroom.kt`
**변경 유형**: 신규

- `NewXroom(ownerEmail: String, targetInfoId: Long, theme: XroomTheme = XroomTheme.CORK_BOARD)`
- `val ownerEmail: Email = Email(ownerEmail)` — 생성자에서 String을 받아 도메인 VO로 변환 (기존 NewPost 패턴)
- 팩토리 메서드 없이 단순 생성자로 충분

### 5.4 Domain - Xroom

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/xroom/domain/Xroom.kt`
**변경 유형**: 신규

- `Xroom(id: Long, ownerEmail: Email, targetInfoId: Long, theme: XroomTheme, createdDate: LocalDateTime)`
- `fun validateOwnership(email: Email)` — 소유권 검증 (기존 MatchingResult.validateOwnership 패턴)
- 향후 PATCH /api/xrooms/{id} (테마 변경 등)에서 활용할 도메인 모델

### 5.5 Domain - XroomCommandRepository (Port)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/xroom/domain/port/XroomCommandRepository.kt`
**변경 유형**: 신규

- `fun save(newXroom: NewXroom): Long`

### 5.6 Domain - XroomQueryRepository (Port)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/xroom/domain/port/XroomQueryRepository.kt`
**변경 유형**: 신규

- `fun existsByTargetInfoId(targetInfoId: Long): Boolean`
- 향후 findOne, findByOwner 등 조회 메서드 추가 예정

### 5.7 Domain - XroomCommandService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/xroom/application/XroomCommandService.kt`
**변경 유형**: 신규

- `fun create(targetInfoId: Long, email: String): Long`
- 흐름:
  1. `targetInfoQueryRepository.findOne(targetInfoId)` — Target-Info 존재 확인
  2. `targetInfo.validateOwnership(Email(email))` — 소유권 검증 (본인의 Target-Info인지)
  3. `xroomQueryRepository.existsByTargetInfoId(targetInfoId)` — 중복 생성 방지, 이미 존재하면 예외
  4. `NewXroom(ownerEmail = email, targetInfoId = targetInfoId)` 생성
  5. `xroomCommandRepository.save(newXroom)` — ID 반환
- 의존성: TargetInfoQueryRepository, XroomCommandRepository, XroomQueryRepository

### 5.8 Domain - ObfuscationType

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/id/ObfuscationType.kt`
**변경 유형**: 수정

- `XROOM("xroom")` enum 값 추가

### 5.9 Domain - EntityType

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/exception/EntityType.kt` (존재 시)
**변경 유형**: 수정

- `XROOM` 값 추가 (예외 메시지용)

### 5.10 Infrastructure - XroomTable

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/entity/table/XroomTable.kt`
**변경 유형**: 신규

- `object XroomTable : BaseTable("XROOMS", "XROOM_ID")`
- 컬럼: `ownerEmail`, `targetInfoId`, `theme`
- 기존 PostTable 패턴과 동일

### 5.11 Infrastructure - XroomEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/entity/XroomEntity.kt`
**변경 유형**: 신규

- 필드: `id`, `ownerEmail`, `targetInfoId`, `theme`, `createdDate`
- `fun toDomain(): Xroom` — Entity → Domain 변환
- `companion object { fun from(row: ResultRow): XroomEntity }` — ResultRow → Entity
- 기존 PostEntity 패턴과 동일

### 5.12 Infrastructure - XroomCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/dao/XroomCommandDao.kt`
**변경 유형**: 신규

- `fun save(newXroom: NewXroom): Long` — `XroomTable.insertAndGetId` 사용
- 기존 PostCommandDao.save() 패턴과 동일

### 5.13 Infrastructure - XroomQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/dao/XroomQueryDao.kt`
**변경 유형**: 신규

- `fun existsByTargetInfoId(targetInfoId: Long): Boolean`
- `XroomTable.select(XroomTable.id).where { XroomTable.targetInfoId eq targetInfoId }.limit(1).any()` 사용 (count 대신 limit+any)

### 5.14 Infrastructure - XroomCommandCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/repository/XroomCommandCoreRepository.kt`
**변경 유형**: 신규

- `@Repository` 어노테이션
- XroomCommandRepository 포트 구현
- XroomCommandDao에 위임

### 5.15 Infrastructure - XroomQueryCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/xroom/repository/XroomQueryCoreRepository.kt`
**변경 유형**: 신규

- `@Repository` 어노테이션
- XroomQueryRepository 포트 구현
- XroomQueryDao에 위임

### 5.16 Infrastructure - RowEntityMapper

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/common/RowEntityMapper.kt`
**변경 유형**: 수정 (필요 시)

- XroomEntity 매핑이 필요한 경우 추가

### 5.17 Boot - CreateXroomRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/xroom/api/request/CreateXroomRequest.kt`
**변경 유형**: 신규

- `data class CreateXroomRequest(val targetInfoId: String)` — 암호화된 ID를 String으로 수신
- `@DecryptId` 어노테이션은 PathVariable에서만 사용되므로, Request Body의 경우 Service에서 복호화하거나 별도 처리 필요
- **대안**: targetInfoId를 쿼리 파라미터로 전달하고 `@DecryptId` 적용. 기존 패턴에서 Request Body 내 ID 복호화 사례가 없으므로, `@RequestParam @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long` 방식 검토

### 5.18 Boot - CreateXroomResponse

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/xroom/api/response/CreateXroomResponse.kt`
**변경 유형**: 신규

- `data class CreateXroomResponse(val xroomId: String)` — 암호화된 ID 반환
- `companion object { fun from(xroomId: Long, idEncryptor: IdEncryptor): CreateXroomResponse }` — ID 암호화 후 응답 생성

### 5.19 Boot - XroomCommandApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/xroom/api/XroomCommandApi.kt`
**변경 유형**: 신규

- `@RestController @RequestMapping("/api/xrooms")`
- `@PostMapping fun create(@AuthenticationPrincipal email: String, @RequestParam @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long): ResponseEntity<CreateXroomResponse>`
- 201 Created 반환
- XroomCommandService.create() 호출 → ID 암호화 → 응답

---

## 6. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `infrastructure/.../script/ddl.sql` | 수정 | XROOMS 테이블 추가 |
| 2 | `domain/.../xroom/domain/XroomTheme.kt` | 신규 | 테마 enum (CORK_BOARD, STRING_LIGHT, DREAMY_BUBBLE) |
| 3 | `domain/.../xroom/domain/NewXroom.kt` | 신규 | X룸 생성 도메인 객체 |
| 4 | `domain/.../xroom/domain/Xroom.kt` | 신규 | X룸 도메인 모델 |
| 5 | `domain/.../xroom/domain/port/XroomCommandRepository.kt` | 신규 | save 포트 |
| 6 | `domain/.../xroom/domain/port/XroomQueryRepository.kt` | 신규 | existsByTargetInfoId 포트 |
| 7 | `domain/.../common/domain/id/ObfuscationType.kt` | 수정 | XROOM 추가 |
| 8 | `domain/.../exception/EntityType.kt` | 수정 | XROOM 추가 (존재 시) |
| 9 | `infrastructure/.../xroom/entity/table/XroomTable.kt` | 신규 | Exposed Table 정의 |
| 10 | `infrastructure/.../xroom/entity/XroomEntity.kt` | 신규 | Entity + toDomain/from |
| 11 | `infrastructure/.../xroom/dao/XroomCommandDao.kt` | 신규 | insertAndGetId |
| 12 | `infrastructure/.../xroom/dao/XroomQueryDao.kt` | 신규 | existsByTargetInfoId |
| 13 | `infrastructure/.../xroom/repository/XroomCommandCoreRepository.kt` | 신규 | 포트 구현 |
| 14 | `infrastructure/.../xroom/repository/XroomQueryCoreRepository.kt` | 신규 | 포트 구현 |
| 15 | `domain/.../xroom/application/XroomCommandService.kt` | 신규 | create 비즈니스 흐름 |
| 16 | `boot/.../xroom/api/response/CreateXroomResponse.kt` | 신규 | 응답 DTO |
| 17 | `boot/.../xroom/api/XroomCommandApi.kt` | 신규 | POST /api/xrooms 엔드포인트 |

---

## 7. 고려사항

- **도메인 분리 근거**: X룸은 자체 애그리거트 루트를 가지며, 테마/블록/공유 등 독립적인 생명주기와 하위 개념이 있다. matching 도메인의 하위 패키지로 넣으면 매칭 도메인이 비대해지고 책임이 섞인다
- **중복 생성 방지**: TARGET_INFO_ID에 UNIQUE INDEX를 걸어 DB 레벨에서 보장하고, 애플리케이션 레벨에서도 existsByTargetInfoId로 사전 검증하여 의미 있는 에러 메시지 제공
- **Claim과의 관계**: api-todo.md 상 X룸 생성은 "지정한 Target-Info 대상으로" 생성한다. Claim 기능이 선행 조건인지는 현재 명시되어 있지 않으므로, Target-Info 소유권만 검증한다. 향후 "claimed된 Target-Info에 대해서만 X룸 생성 가능" 제약이 필요하면 Service에 조건 추가만 하면 됨
- **테마 확장**: enum으로 설계하여 새 테마 추가 시 enum 값만 추가. 현재는 기본값 CORK_BOARD로 고정이므로 Request에서 테마를 받지 않음. 향후 테마 선택 기능 추가 시 Request Body에 optional theme 필드 추가
- **FK 미사용**: TARGET_INFO_ID는 FK를 걸지 않고 UNIQUE INDEX만 사용. 프로젝트 정책에 따라 애플리케이션 레벨에서 참조 무결성 보장
- **Request Body vs Query Parameter**: targetInfoId 전달 방식. 기존 프로젝트에서 `@DecryptId`는 `@PathVariable`에 사용되는 패턴이 확인됨. `@RequestParam`에도 적용 가능한지 확인 필요. 불가능하면 Request Body에서 String으로 받아 Service에서 복호화하는 방식으로 대체
- **성능**: OWNER_EMAIL 인덱스로 "내 X룸 조회"(GET /api/xrooms/me) 대비, TARGET_INFO_ID UNIQUE 인덱스로 중복 방지 + 조회 성능 확보

---

## 8. 테스트

| 대상 | 테스트 내용 |
|------|-------------|
| NewXroom | ownerEmail String → Email 변환 검증 |
| XroomTheme | enum 값 존재 확인 |
| XroomCommandService | create 흐름 (TargetInfo 존재 확인, 소유권 검증, 중복 방지, save) |
| XroomCommandDao | save DB 반영 확인 |
| XroomQueryDao | existsByTargetInfoId 조건 검증 |
| XroomCommandApi | POST /api/xrooms REST Docs |

---

## 9. 확인 필요 사항

1. **`@DecryptId`가 `@RequestParam`에서도 동작하는지**: PathVariable 외에 사용 가능한지 코드 확인 필요. 불가능하면 targetInfoId를 Path에 포함시키거나(`POST /api/xrooms?targetInfoId=xxx`), Request Body에서 String으로 받아 수동 복호화
2. **Claim 선행 조건 여부**: "Target-Info에 대해 X룸 생성"이 Claim 완료를 전제로 하는지, 아니면 Claim 없이도 자유롭게 생성 가능한지 비즈니스 확인 필요
