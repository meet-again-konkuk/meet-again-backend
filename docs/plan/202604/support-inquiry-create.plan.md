# Plan: 1:1 문의 접수 API

> 작성일: 2026-04-13

## 1. 개요

인증된 사용자가 1:1 문의를 접수할 수 있는 `POST /api/domain/inquiries` API를 구현한다. community 도메인의 게시글 작성(PostCommand) 패턴을 그대로 따른다.

## 2. 변경 전략

기존 community/PostCommand 패턴(NewPost → PostCommandService → PostCommandRepository → PostCommandDao)과 동일한 구조를 적용한다.

| 레이어 | 구성 요소 | 역할 |
|--------|-----------|------|
| Boot (Api) | `InquiryCommandApi` | 요청 수신, Request 검증, Service 호출, 201 응답 |
| Boot (Request) | `NewInquiryRequest` | Bean Validation + `toNewInquiry(email)` 변환 |
| Boot (Response) | `NewInquiryResponse` | `inquiryId: Long` 반환 |
| Domain (Model) | `NewInquiry` | 도메인 모델, init에서 title/content 유효성 검증, `authorEmail: Email` |
| Domain (Port) | `InquiryCommandRepository` | `fun save(newInquiry: NewInquiry): Long` |
| Domain (Service) | `InquiryCommandService` | Repository 조합만 담당 |
| Infra (Table) | `InquiryTable` | Exposed 테이블 정의, BaseTable 상속 |
| Infra (Dao) | `InquiryCommandDao` | `insertAndGetId` 실행 |
| Infra (Repository) | `InquiryCommandCoreRepository` | 포트 구현체, Dao에 위임 |
| DDL | `ddl.sql` | `SUPPORT_INQUIRIES` 테이블 추가 |

### Request → Domain 변환 규칙

- `NewInquiryRequest.toNewInquiry(email: String)` → `NewInquiry(authorEmail = email, title = title, content = content)` 생성
- `NewInquiry`에서 `authorEmail`은 String으로 받아 내부에서 `Email(authorEmail)`로 변환 (도메인 객체 변환은 도메인 내부에서)
- `NewInquiry.init`에서 title, content 유효성 검증

### Validation 상수

`ValidationMessages`에 추가할 상수:
- `INQUIRY_TITLE_REQUIRED` = "문의 제목은 필수입니다."
- `INQUIRY_TITLE_SIZE` = "문의 제목은 50자 이하여야 합니다."
- `INQUIRY_CONTENT_REQUIRED` = "문의 내용은 필수입니다."
- `INQUIRY_CONTENT_SIZE` = "문의 내용은 500자 이하여야 합니다."

## 3. 변경 파일 목록

### Phase 1: DDL + Validation 상수

| # | 파일 | 내용 |
|---|------|------|
| 1 | `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql` | `SUPPORT_INQUIRIES` 테이블 DDL 추가 |
| 2 | `boot/ma-boot-web/src/main/kotlin/.../support/validation/ValidationPatterns.kt` | `ValidationMessages`에 `INQUIRY_TITLE_REQUIRED`, `INQUIRY_TITLE_SIZE`, `INQUIRY_CONTENT_REQUIRED`, `INQUIRY_CONTENT_SIZE` 추가 |

### Phase 2: Domain (ma-domain-core)

| # | 파일 | 내용 |
|---|------|------|
| 3 | `domain/ma-domain-core/src/main/kotlin/.../domain/support/domain/NewInquiry.kt` | 신규. `authorEmail: Email`, `title: String`, `content: String`. init에서 검증. `MAX_TITLE_LENGTH = 50`, `MAX_CONTENT_LENGTH = 500` 상수 |
| 4 | `domain/ma-domain-core/src/main/kotlin/.../domain/support/domain/port/InquiryCommandRepository.kt` | 신규. `fun save(newInquiry: NewInquiry): Long` |
| 5 | `domain/ma-domain-core/src/main/kotlin/.../domain/support/application/InquiryCommandService.kt` | 신규. `@Service @Transactional`. `fun create(newInquiry: NewInquiry): Long` — Repository.save 호출 |

### Phase 3: Infrastructure (ma-db-core)

| # | 파일 | 내용 |
|---|------|------|
| 6 | `infrastructure/storage/ma-db-core/src/main/kotlin/.../domain/support/entity/table/InquiryTable.kt` | 신규. `BaseTable("SUPPORT_INQUIRIES", "SUPPORT_INQUIRY_ID")`. 컬럼: `authorEmail`, `title`, `content` |
| 7 | `infrastructure/storage/ma-db-core/src/main/kotlin/.../domain/support/dao/InquiryCommandDao.kt` | 신규. `fun save(newInquiry: NewInquiry): Long` — `insertAndGetId` |
| 8 | `infrastructure/storage/ma-db-core/src/main/kotlin/.../domain/support/repository/InquiryCommandCoreRepository.kt` | 신규. `@Repository`. InquiryCommandRepository 구현, Dao에 위임 |

### Phase 4: Boot (ma-boot-web)

| # | 파일 | 내용 |
|---|------|------|
| 9 | `boot/ma-boot-web/src/main/kotlin/.../domain/support/api/request/NewInquiryRequest.kt` | 신규. `title: @NotBlank @Size(max=50)`, `content: @NotBlank @Size(max=500)`. `fun toNewInquiry(authorEmail: String): NewInquiry` |
| 10 | `boot/ma-boot-web/src/main/kotlin/.../domain/support/api/response/NewInquiryResponse.kt` | 신규. `inquiryId: Long` |
| 11 | `boot/ma-boot-web/src/main/kotlin/.../domain/support/api/InquiryCommandApi.kt` | 신규. `@RestController @RequestMapping("/api/domain/inquiries")`. `@PostMapping @ResponseStatus(CREATED)`. `@AuthenticationPrincipal email` |

## 4. 고려사항

- **새 도메인 패키지**: 기존에 support 도메인이 없으므로, 각 모듈에 `support` 패키지를 신규 생성한다. memory의 "새로운 도메인 분리 지양" 피드백이 있으나, 문의(inquiry)는 기존 도메인(auth, community, matching, member) 어디에도 속하지 않는 독립 도메인이므로 신규 패키지가 적절하다.
- **FK 미사용**: `SUPPORT_INQUIRIES` 테이블에서 `AUTHOR_EMAIL`은 MEMBERS 테이블과 FK 없이 INDEX만 건다.
- **content 길이 제한**: 500자로 제한. DB 컬럼은 VARCHAR(1500) 사용 (한글 기준 여유 확보).
- **DB 스키마**: `SUPPORT_INQUIRIES` 테이블 신규 생성. PK: `SUPPORT_INQUIRY_ID`, INDEX: `AUTHOR_EMAIL`.
- **응답 코드**: POST 생성이므로 `201 Created` 반환 (기존 PostCommandApi 패턴과 동일).

### DDL 설계

```
SUPPORT_INQUIRIES
- SUPPORT_INQUIRY_ID (PK, BIGINT AUTO_INCREMENT)
- AUTHOR_EMAIL (VARCHAR(255), NOT NULL)
- TITLE (VARCHAR(100), NOT NULL)
- CONTENT (VARCHAR(1500), NOT NULL)
- BaseTable 공통 컬럼 (CREATED_DATE, CREATED_BY, LAST_MODIFIED_DATE, LAST_MODIFIED_BY, DELETED)
- INDEX idx_support_inquiry_author_email (AUTHOR_EMAIL)
```

## 5. 검증 항목

- [ ] `./gradlew build` 성공
- [ ] `NewInquiry` 도메인 모델에 Spring 의존성 없음
- [ ] `NewInquiry` init에서 title 빈값/50자 초과 시 예외 발생
- [ ] `NewInquiry` init에서 content 빈값/500자 초과 시 예외 발생
- [ ] `POST /api/domain/inquiries` 인증 없이 호출 시 401 응답
- [ ] 유효한 요청 시 201 응답 + inquiryId 반환
- [ ] title 빈값/50자 초과 시 400 응답
- [ ] content 빈값/500자 초과 시 400 응답
