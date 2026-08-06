# Plan: REQ-015 — X룸 + 기억 원자적 일괄 생성 (`POST /api/xrooms/with-memories`)

- 작성일: 2026-08-05
- 작업 유형: 기능 개발 (프론트 장애 리포트 대응)
- 대상 저장소: meet-again-backend (base 브랜치 `develop`)
- 스펙 단일 소스: 프론트 `origin/develop:docs/to-backend.md` 의 **REQ-015** (af751f4, 2026-07-30)

---

## 1. 배경 — 무엇이 깨졌나

프론트의 "기억의 방 저장하기" 버튼 하나가 백엔드 호출 3종으로 쪼개져 있고, **트랜잭션이 방 생성에서 끊긴다.**

| 순서 | 호출 | 트랜잭션 경계 |
|------|------|----------------|
| 1 | `POST /api/xrooms` | `XroomCommandService.create` — 여기서 커밋 |
| 2 | `POST /api/xrooms/{xroomId}/memories` (기억 수만큼 반복) | `MemoryCommandService.addMemory` — 호출마다 별도 커밋 |
| 3 | `POST .../memories/{memoryId}/photo` (사진 있을 때) | `MemoryPhotoCommandService.uploadPhoto` — 별도 커밋 |

2단계가 실패하면 **기억 0개짜리 `XROOMS` row가 커밋된 채로 남는다.** 사용자는 저장 실패로 인식하고 다시 누르지만, 재시도는 아래에서 막힌다.

- `XroomValidator.validateNotDuplicated` (`XroomValidator.kt:51-55`) — `xroomQueryRepository.exists(targetInfoId)` 가 참이면 무조건 `DuplicateException` → 409.
- `XroomCommandApi` 에 **DELETE 엔드포인트가 없다** (`POST`, `PATCH` 뿐) → 잔여 빈 방을 지울 방법이 없다.

결과: **해당 `targetInfoId` 에 대해 사용자가 영구히 방을 못 만든다.** 프론트가 첨부한 스택트레이스가 이 경로 그대로다.

### 확인된 사실 (코드 대조)

| 확인 항목 | 결과 | 근거 |
|-----------|------|------|
| 방 생성과 기억 생성이 다른 트랜잭션인가 | 예 | `XroomCommandService.create` 가 `save` 후 종료, 별도 HTTP 콜 |
| 중복 검사가 soft-deleted 방도 세는가 | 아니오 (`activeRows` 필터) | `XroomQueryDao.exists` (`XroomQueryDao.kt:30-35`) |
| 방 삭제 경로가 있는가 | 없음 | `XroomCommandApi` = `@PostMapping`, `@PatchMapping("/{xroomId}")` |
| `GET /api/xrooms/me` 에 `targetInfoId` 가 있는가 (프론트 대안 A) | **이미 있음** | `MyXroomResponse.kt:14` (`@EncryptId(TARGET_INFO)`) |

> 대안 A는 이미 충족돼 있다. 프론트가 몰랐거나 회신을 못 받은 상태이므로 **회신에 명시**한다.

---

## 2. 목표

`POST /api/xrooms/with-memories` 하나로 **방 + 기억 N개 + 감정태그를 단일 트랜잭션에서 생성**한다. 기억 하나라도 실패하면 `XROOMS`·`MEMORIES`·`MEMORY_EMOTION_TAGS` 전부 롤백해 잔여 row를 남기지 않는다. 사진은 스펙대로 이 API 밖에 두고 기존 photo API를 그대로 쓴다.

기존 `POST /api/xrooms`, `POST /api/xrooms/{xroomId}/memories` 는 **호환용으로 유지**한다 (스펙 요청 사항, 작성자 수정 화면이 계속 사용).

---

## 3. API 계약

- Method: `POST`
- Path: `/api/xrooms/with-memories`
- Auth: `Bearer required`
- Content-Type: `application/json`
- Status: `201 Created`

### Request

```json
{
  "targetInfoId": "암호화된 TARGET_INFO ID",
  "finalMessage": "마지막으로 남길 말",
  "memories": [
    {
      "title": "처음 만난 날",
      "eventDate": "2024-05-01",
      "eventDatePrecision": "DAY",
      "location": "연남동 카페",
      "emotionTags": ["설렘", "고마움"],
      "text": "그날의 기록글",
      "letter": null
    }
  ]
}
```

### Response (201)

```json
{
  "xroomId": "암호화된 XROOM ID",
  "memoryIds": ["암호화된 MEMORY ID 1", "암호화된 MEMORY ID 2"]
}
```

- `memoryIds` 순서 = 요청 `memories` 배열 순서 (프론트가 사진 업로드 대상을 인덱스로 매칭하므로 **순서 보장은 계약**이다).
- 각 memory 의 검증 규칙은 기존 `POST /api/xrooms/{xroomId}/memories` 와 동일 — `MemoryDetails.of` 재사용으로 자동 보장.

---

## 4. 설계 결정

### D1. 기억 목록은 일급 컬렉션으로 받는다

요청 DTO는 raw 값만 받고 VO 생성은 도메인이 한다는 프로젝트 컨벤션(`AddMemoryRequest.toNewMemory` 선례)을 유지한다. 다만 `NewMemory` 는 `xroomId` 를 요구하는데 그 값은 방 insert 이후에야 생긴다. 따라서 **`xroomId` 이전 단계의 검증된 값 묶음**을 담을 일급 컬렉션을 신설한다.

- `NewMemories(val data: List<MemoryDetails>)` — 멤버 변수명 `data` 로 통일 (컨벤션).
- `init` 에서 최소 1개 검증 (스펙: `memories` 는 최소 1개 이상).
- `bindTo(xroomId: Long): List<NewMemory>` — xroomId 를 묶어 `NewMemory` 목록으로 전개.

### D2. 서비스는 flat 위임만 유지한다

"application 서비스는 flat 위임 + result 인라인만, private 헬퍼·인라인 변환 알고리즘 금지" 컨벤션에 맞춰, 반복 저장은 **포트 메서드로 내린다.**

- `MemoryCommandRepository.saveAll(newMemories: List<NewMemory>): List<Long>` 신설.
- 서비스는 `validate → save(방) → saveAll(기억) → 결과 조립` 4줄 flat.

### D3. `saveAll` 은 batchInsert 대신 건별 insert 를 반복한다

`MemoryCommandDao.save` 는 `insertAndGetId` 로 memory id 를 받아 `MemoryEmotionTagTable` 에 태그를 batchInsert 한다. memory 자체를 batchInsert 하면 MariaDB에서 생성 키 순서 보장이 불안정해 **`memoryIds` 순서 계약(§3)을 깨뜨릴 위험**이 있다. 방 하나당 기억 수는 수십 건 규모이므로 건별 insert 반복이 안전하고 충분하다. `saveAll` 은 기존 `save` 를 순회 호출하는 형태로 둔다.

### D4. `@EncryptId` 는 현재 `List<Long>` 필드를 직렬화하지 못한다 — 지원 추가 필요

`EncryptIdSerializer` 는 `JsonSerializer<Long>` 이고, `EncryptIdAnnotationIntrospector` 는 `findSerializer`/`findDeserializer` 만 오버라이드한다. Jackson 에서 컬렉션 **원소** 직렬화는 `findContentSerializer` 로 결정되므로, `@EncryptId(MEMORY) val memoryIds: List<Long>` 은 그대로 두면 동작하지 않는다.

- 조치: `EncryptIdAnnotationIntrospector` 에 `findContentSerializer` 오버라이드 추가 (대칭성을 위해 `findContentDeserializer` 도 함께).
- 대안(응답을 `List<MemoryResponse>` 객체 배열로 변경)은 스펙의 flat string 배열 계약을 깨므로 채택하지 않는다.
- 이 변경은 support 레이어 공용이므로 **단위 테스트를 반드시 동반**한다 (`EncryptIdSerializerTest` 확장).

### D5. 기억 개수 상한을 둔다

트랜잭션 길이와 요청 본문 크기를 묶기 위해 상한을 둔다. 스펙에 상한 언급이 없으므로 백엔드가 정하고 회신에 명시한다. **`NewMemories` init 에서 최대 50개** 로 제안 — 초과 시 `InvalidValueException` → 400.

---

## 5. 변경 파일

### 신규

| 파일 | 모듈 | 내용 |
|------|------|------|
| `domain/xroom/domain/memory/NewMemories.kt` | domain-core | 일급 컬렉션 `NewMemories(val data: List<MemoryDetails>)`. `init` 에서 최소 1개·최대 50개 검증. `bindTo(xroomId: Long): List<NewMemory>` |
| `domain/xroom/domain/CreatedXroom.kt` | domain-core | 생성 결과 값 객체 — `xroomId: Long`, `memoryIds: List<Long>` (계획 초안의 `XroomWithMemories` 에서 리네임 — §14 참조) |
| `domain/xroom/api/request/CreateXroomWithMemoriesRequest.kt` | boot-web | `@field:EncryptId(TARGET_INFO) targetInfoId: Long`, `finalMessage: String?`, `memories: List<AddMemoryRequest>`. `toNewMemories(): NewMemories` |
| `domain/xroom/api/response/XroomWithMemoriesResponse.kt` | boot-web | `@EncryptId(XROOM) xroomId: Long`, `@EncryptId(MEMORY) memoryIds: List<Long>`. `companion from(createdXroom: CreatedXroom)` |

### 수정

| 파일 | 모듈 | 변경 |
|------|------|------|
| `domain/xroom/api/XroomCommandApi.kt` | boot-web | `@PostMapping("/with-memories")` + `@ResponseStatus(CREATED)` 인 `createWithMemories(@LoginMember, @RequestBody)` 추가 |
| `domain/xroom/api/request/AddMemoryRequest.kt` | boot-web | `toMemoryDetails(): MemoryDetails` 추출, 기존 `toNewMemory(xroomId)` 가 이를 위임하도록 변경 (동작 불변) |
| `domain/xroom/application/XroomCommandService.kt` | domain-core | `createWithMemories(memberId: Long, targetInfoId: Long, finalMessage: String?, newMemories: NewMemories): CreatedXroom` 추가. `MemoryCommandRepository` 주입 추가 |
| `domain/xroom/domain/port/MemoryCommandRepository.kt` | domain-core | `saveAll(newMemories: List<NewMemory>): List<Long>` 추가 |
| `domain/xroom/dao/MemoryCommandDao.kt` | db-core | `saveAll(newMemories: List<NewMemory>): List<Long>` — 기존 `save` 순회 (D3) |
| `domain/xroom/repository/MemoryCommandCoreRepository.kt` | db-core | `saveAll` 을 DAO로 위임 |
| `support/id/EncryptIdAnnotationIntrospector.kt` | boot-web | `findContentSerializer`·`findContentDeserializer` 오버라이드 (D4) |

---

## 6. 트랜잭션 경계

`XroomCommandService` 는 클래스 레벨 `@Transactional` 이므로 `createWithMemories` 한 메서드가 곧 하나의 트랜잭션이다. 추가 설정 없이 요구사항을 만족한다.

```
createWithMemories  ── TX 시작
  xroomValidator.validate(newXroom)      // 소유권 + 중복 → 실패 시 롤백 (insert 전이라 잔여 없음)
  xroomCommandRepository.save(newXroom)  // XROOMS insert
  memoryCommandRepository.saveAll(...)   // MEMORIES + MEMORY_EMOTION_TAGS insert
                    ── TX 커밋 / 예외 시 전부 롤백
```

`MemoryDetails.of` 검증은 **컨트롤러 바인딩 시점**(`toNewMemories()`)에 이미 끝나므로, 잘못된 기억은 트랜잭션 진입 전에 400으로 거절된다. 트랜잭션 안에서 남는 실패는 DB 예외뿐이고 이는 전부 롤백된다.

---

## 7. 예외 → HTTP 매핑

| 상황 | 도메인 예외 | HTTP |
|------|-------------|------|
| targetInfoId 가 내 것이 아님 | `AccessDeniedException` | 403 |
| targetInfoId 없음 | `EntityNotFoundException` | 404 |
| 같은 targetInfoId 활성 방 존재 | `DuplicateException` | 409 |
| `memories` 비었음 / 50개 초과 | `InvalidValueException` | 400 |
| title·eventDate·text/letter 검증 실패 | `InvalidValueException` | 400 |

기존 `GlobalExceptionHandler` 매핑을 그대로 타므로 핸들러 변경은 없다.

---

## 8. 테스트

TDD 순서: 스켈레톤 → 테스트 RED 관측 → 구현 GREEN.

### API E2E (`XroomCommandApiTest` 에 context 추가 — 클래스 분리하지 않음)

| 케이스 | 기대 |
|--------|------|
| 방 + 기억 2개 일괄 생성 성공 | 201, `xroomId`·`memoryIds` 2개, `MEMORIES`·`MEMORY_EMOTION_TAGS` DB 반영 |
| `memoryIds` 순서 = 요청 순서 | 응답 순서와 DB `title` 순서 일치 |
| 기억 하나가 유효하지 않음 (eventDate 형식 오류) | 400, **`XROOMS` row 0건** ← 핵심 회귀 방지 |
| `memories` 빈 배열 | 400 |
| 이미 같은 targetInfoId 활성 방 존재 | 409 (기존 정책 유지) |
| 남의 targetInfoId | 403 |

DB 상태 검증은 Service 빈 주입 없이 `transaction { }` 으로 테이블을 직접 읽는다 (컨벤션). 요청 본문은 inline `mapOf` 로 구성한다.

### 단위 테스트

| 대상 | 케이스 |
|------|--------|
| `NewMemories` | 빈 목록 예외, 51개 예외, `bindTo` 가 xroomId 를 전부 주입 |
| `EncryptIdSerializer` (확장) | `List<Long>` 필드가 암호화 문자열 배열로 직렬화 |

### 롤백 검증 주의

"기억 생성 중 실패 → 전체 롤백" 을 E2E로 관측하려면 실패가 **트랜잭션 안에서** 나야 한다. §6대로 검증은 트랜잭션 전에 끝나므로, 이 케이스의 관측 지점은 "400 응답 + `XROOMS` 0건" 이다. 트랜잭션 내부 롤백 자체는 `MemoryCommandRepository` 를 실패시키는 통합 테스트로 별도 확인한다.

---

## 9. REST Docs / 문서 동기화

| 대상 | 작업 |
|------|------|
| `boot/ma-boot-web/src/docs/asciidoc/xroom/create-xroom-with-memories.adoc` | 신규 |
| `boot/ma-boot-web/src/docs/asciidoc/main.adoc` | `[[xroom-create-with-memories]]` 앵커 + 링크 추가 (`[[xroom-create]]` 바로 뒤) |
| `XroomVocabulary.kt` | `memories[]` 하위 필드·`memoryIds` 필드 정의 추가 (기존 add-memory 정의 재사용) |
| `docs/api-todo.md` | X룸 섹션 현행화 |
| 프론트 `docs/to-backend.md` REQ-015 `Backend Reply` | 회신 내용은 §11 |

---

## 10. 결정 사항

### ✅ 잔여 빈 방 복구 — **A안 확정 (2026-08-05, 사용자 승인)**

이 API를 배포해도 **과거 실패로 이미 생긴 빈 방은 그대로 남아 계속 409를 유발한다.** `XroomQueryDao.exists` 가 `activeRows` 필터를 타므로 soft delete 만 되면 해소된다. 선택지:

| 안 | 내용 | 프론트 변경 | 스펙 정합성 |
|----|------|-------------|-------------|
| **A ✅ 채택** | `createWithMemories` 가 같은 targetInfoId 의 **기억 0개인 활성 방**을 발견하면 soft delete 후 진행. 기억이 있는 방이면 기존대로 409 | 불필요 | 스펙 Acceptance #6("활성 방 있으면 409")을 빈 방에 한해 완화 — 회신에 명시 필요 |
| B | `DELETE /api/xrooms/{xroomId}` (본인·빈 방만) 신설 = 프론트가 제시한 대안 B | 필요 (정리 UI) | 스펙에 명시된 대안 |
| C | 일회성 정리 스크립트로 기존 빈 방만 제거 | 불필요 | 재발 시 다시 막힘 |

채택 근거: 빈 방은 정의상 저장 실패의 잔재이지 사용자가 만든 방이 아니고, 프론트 변경 없이 **기존에 막힌 사용자까지 즉시 풀린다.**

#### A안 구현 상세

빈 방 판별과 정리는 **repo 조회가 필요한 사전 검증**이므로 `XroomValidator` 에 둔다 (Service 는 위임만 — 컨벤션).

| 대상 | 변경 |
|------|------|
| `XroomValidator` | `validateNotDuplicated` 를 "빈 방이면 정리하고 통과, 기억 있으면 409" 로 확장. `MemoryQueryRepository.count` 로 기억 수 판별, 빈 방은 `XroomCommandRepository` 로 soft delete |
| `XroomCommandRepository` | 현재 `delete(ownerId: Long)` 뿐 — **`deleteById(xroomId: Long, memberId: Long)` 추가 필요** |
| `XroomCommandDao` | `softDelete({ XroomTable.id eq xroomId }, memberId)` 추가 |

**적용 범위**: 신규 `createWithMemories` 에만 적용한다. 기존 `POST /api/xrooms` 는 계약 변경 없이 그대로 409 (호환 유지 요청 사항).

**테스트 추가**: 빈 방이 있는 상태에서 일괄 생성 → 201 + 기존 방 `deleted=1` + 새 방 생성 / 기억 있는 방이 있는 상태 → 409.

### 기억 개수 상한 50 (D5)

스펙에 없어 백엔드가 정하는 값이다. 다른 수치를 원하면 조정한다.

---

## 11. 프론트 회신 초안 (`to-backend.md` REQ-015 Backend Reply)

- Implemented endpoint: `POST /api/xrooms/with-memories`
- Transaction boundary: `XroomCommandService.createWithMemories` 단일 트랜잭션 — 방·기억·감정태그 전부 롤백
- Photo handling policy: 스펙대로 분리 유지. 응답 `memoryIds` 순서 = 요청 `memories` 순서 (계약)
- Recovery/delete policy: **기억 0개인 활성 방은 일괄 생성 시 자동 정리(soft delete) 후 진행.** 기억이 있는 방이면 기존대로 409 → 스펙 Acceptance #6 을 "기억이 있는 활성 방" 기준으로 읽어달라는 요청 포함. 별도 DELETE 엔드포인트는 신설하지 않음
- DB migration: 불필요 (기존 테이블 재사용)
- 추가 회신: **대안 A는 이미 충족돼 있음** — `GET /api/xrooms/me` 응답에 `targetInfoId` 가 이미 포함돼 있다 (`MyXroomResponse`, 암호화 ID)
- 추가 회신: `memories` 최대 개수 50 (백엔드 정의)

---

## 12. 작업 순서

1. ~~§10 결정 확정 (복구 방식)~~ → **A안 확정 완료 (2026-08-05)**
2. ~~브랜치 `feat/xroom-create-with-memories` 생성~~ → **완료 (2026-08-05, base `develop`)**
3. 스켈레톤 → 테스트 RED 관측 → 구현 GREEN
4. `EncryptId` content serializer 지원 + 단위 테스트 (D4)
5. code-reviewer 리뷰
6. REST Docs + `main.adoc` + `api-todo.md`
7. 로컬 실기동 검증 (실 HTTP: 성공 / 400 후 `XROOMS` 0건 / 409)
8. 커밋 → 푸시 → PR (base: `develop`)

---

## 13. 구현 착수 시 확정 사항 (2026-08-05)

착수 전 코드 대조에서 §5 표가 열어둔 지점 3개를 확정했다. **아래가 시그니처 계약이다** — 스켈레톤·테스트·구현이 모두 이 절을 따른다.

### D6. 빈 방 조회용 포트 메서드를 신설한다

A안은 빈 방의 **id** 가 필요한데 `XroomQueryRepository` 에는 `exists(targetInfoId): Boolean` 뿐이라 id 를 얻을 수 없다.

| 대상 | 추가 |
|------|------|
| `XroomQueryRepository` (port) | `findByTargetInfoIdOrNull(targetInfoId: Long): Xroom?` |
| `XroomQueryCoreRepository` | DAO 위임 |
| `XroomQueryDao` | `findByTargetInfoId(targetInfoId: Long): XroomEntity?` — `activeRows` 필터 |

`...OrNull` 접미사는 이 프로젝트의 널러블 포트 관례를 따른 것이다 (`SmsRepository.findOrNull`, `BlockQueryRepository.findExistingActiveOrNull`, `PointHistoryRepository.findOneOrNull` 선례).

### D7. A안은 신규 메서드에만 담는다 — 기존 `validate` 는 불변

§10 의 "적용 범위: 신규 `createWithMemories` 에만" 을 코드로 강제하기 위해, `validateNotDuplicated` 를 **확장하지 않고** 별도 진입점을 만든다.

| 메서드 | 사용처 | 중복 시 동작 |
|--------|--------|--------------|
| `validate(newXroom)` (기존, 무변경) | `POST /api/xrooms` | 무조건 `DuplicateException` (409) |
| `validateAfterRemovingEmptyRoom(newXroom)` (신규) | `POST /api/xrooms/with-memories` | 기억 0개면 soft delete 후 통과, 있으면 409 |

신규 메서드 절차: 소유권 검증(기존 private 재사용) → `findByTargetInfoIdOrNull` → null 이면 통과 → `memoryQueryRepository.count(setOf(xroom.id))` 를 **`MemoryCounts` 로 포장**해 판별 → 0 이면 `xroomCommandRepository.deleteById(xroom.id, newXroom.ownerId)` 후 통과, 아니면 409.

`XroomValidator` 에 `XroomCommandRepository` 주입이 추가된다. 포트가 `Map<Long, Int>` 를 주면 반드시 일급 컬렉션으로 포장해서 쓴다 (컨벤션).

### D8. E2E 는 `XroomIntegrationTest` 로 간다 (§8 정정)

§8 은 "`XroomCommandApiTest` 에 context 추가" 라고 적었지만, 그 클래스는 `@WebMvcTest` + `@MockkBean XroomCommandService` 라 **DB row 를 관측할 수 없다.** 핵심 회귀 방지 케이스("400 후 `XROOMS` 0건")가 검증 불가능해지므로 아래로 나눈다.

| 테스트 | 클래스 | 범위 |
|--------|--------|------|
| REST Docs 문서화 1건 | `XroomCommandApiTest` (`@WebMvcTest`) | snippet `xroom/create-xroom-with-memories` 생성 |
| DB 관측 E2E 전부 | `XroomIntegrationTest` (`@SpringBootTest`, 실 스키마) | 201·순서 계약·400 후 `XROOMS` 0건·빈 배열 400·409·빈 방 자동 정리·403·401 |

DB 상태는 Service 빈 주입 없이 `transaction { }` 으로 테이블을 직접 읽는다. 요청 본문은 inline `mapOf`.

### 베이스라인

착수 직전 `./gradlew test` = **BUILD SUCCESSFUL (exit 0)**. 이후 발생하는 실패는 전부 이번 변경에 귀속된다.

---

## 14. 구현 결과 (2026-08-05)

TDD 파이프라인(스켈레톤 → RED 관측 → GREEN → 리뷰)으로 완료했다. RED 관측 시점 **신규 21 실패 / 기존 실패 0**, 최종 **전체 GREEN**.

### 계획과 달라진 지점

| 항목 | 계획 | 실제 | 근거 |
|------|------|------|------|
| `EncryptId` content serializer (D4) | `findContentSerializer`·`findContentDeserializer` 오버라이드 **추가**만 하면 됨 | 추가 + **기존 `findSerializer`·`findDeserializer` 에 컨테이너 가드** 필요 | Jackson 은 프로퍼티 직렬화기를 `findSerializer` 에서 **먼저** 결정한다. 가드가 없으면 `@EncryptId List<Long>` 이 여전히 리스트 통째로 `JsonSerializer<Long>` 에 넘어가 `ClassCastException`. 가드는 `type?.isContainerType == true` 로 널 안전해야 한다 — `DeserializerCache` 가 넘기는 루트 `AnnotatedClass` 는 `getType()` 이 null 이라 `!!` 면 전 API 직렬화가 NPE 로 죽는다 |
| 빈 방 판별 (D7) | Validator 가 `MemoryCounts` 로 판별 | 동일하되 **`MemoryCounts.hasNoMemory(xroomId)` 신설** | "상태 판단은 도메인 객체 내부에서" 컨벤션 — `countOf(...) == 0` 비교가 Validator 로 새지 않게 함 |
| 롤백 검증 (§8) | "`MemoryCommandRepository` 를 실패시키는 통합 테스트로 별도 확인" | `XroomTransactionIntegrationTest` 신설로 이행 | 도메인 검증 한도와 DB 컬럼 폭이 동일해(title 200/200, tag 50/50) API 입력만으로는 트랜잭션 **내부** 실패를 만들 수 없다 |
| 생성 결과 값 객체 이름 (§5) | `XroomWithMemories` | **`CreatedXroom`** | 레포의 `XxxWithYyy` 는 예외 없이 도메인 객체를 필드로 보유하는 합성 타입(`PostWithAuthor(val post: Post, …)`, `MatchingResultWithProfile`)이라 원시 ID만 담는 이 값 객체에는 맞지 않다. 입력측 `NewXroom` 과 대칭을 이루는 `CreatedXroom` 으로 확정 (`XxxResult` 규약은 "Result 지양" 사용자 지침에 따라 배제) |

### 테스트 구성

| 클래스 | 범위 |
|--------|------|
| `NewMemoriesTest` (신규) | 빈 목록·51개 예외, 1개·50개 경계, `bindTo` 의 xroomId 주입·순서 보존·필드 무손실 |
| `XroomValidatorTest` (context 추가) | 방 없음 통과 / 빈 방 soft delete 후 통과 / 기억 있으면 409·삭제 안 함 / 소유권 실패는 방 조회 전에 차단 |
| `MemoryCountsTest` (context 추가) | `hasNoMemory` — 키 없음·빈 맵·0·1 이상 |
| `EncryptIdAnnotationIntrospectorTest` (신규) | `List<Long>` 원소 단위 암호화·순서 보존, 빈 배열, **단일 `Long` 회귀 가드** |
| `XroomIntegrationTest` (context 추가, 9건) | 201+DB 반영 / `memoryIds` 순서 계약 / 400 후 `XROOMS` 0건 / 빈 배열 400 / 51개 400 / 409 / 빈 방 자동 정리 후 201 / 403 / 401 |
| `XroomTransactionIntegrationTest` (신규, 2건) | 기억 행을 실제로 insert 한 뒤 실패시켜 **`XROOMS`·`MEMORIES` 전부 롤백(0건)** 관측 + 정상 위임 대조군(1건/2건) |
| `XroomCommandApiTest` (test 추가) | REST Docs snippet `xroom/create-xroom-with-memories` |

롤백 테스트는 예외를 checked(`IOException`)로 바꿔 돌려 **실패하는 것까지 확인**했다 (Spring 기본 롤백 규칙이 unchecked 한정 → 커밋됨 → `expected:<0L> but was:<1L>`). 통과 시의 0건이 "원래 안 만들어져서"가 아니라 롤백 결과임이 보장된다.
