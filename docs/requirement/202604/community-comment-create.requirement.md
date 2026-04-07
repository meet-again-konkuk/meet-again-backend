# Plan: POST /api/community/posts/{postId}/comments — 댓글 작성

## Context
커뮤니티 게시글에 댓글을 작성하는 API를 구현한다. 기존 `PostCommandService.create()` 패턴을 따르되, 업데이트된 스킬 규칙(SOLID, 포트는 List 반환, 일급 컬렉션은 행위 있을 때만)을 적용한다.

## 구현 파일 목록 (순서대로)

### 1. DDL 추가
**수정**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`
- `COMMUNITY_COMMENTS` 테이블 추가
- 컬럼: POST_ID, AUTHOR_EMAIL, AUTHOR_NICKNAME, CONTENT + BaseTable 공통
- 인덱스: `idx_community_comment_post_id (POST_ID)`
- FK 사용 금지

### 2. 도메인 모델 — NewComment
**신규**: `domain/ma-domain-core/.../community/domain/NewComment.kt`
- postId, authorEmail, authorNickname, content
- init 블록에서 content 유효성 검증 (비어있으면 안됨, 500자 이하)
- `NewPost` 패턴과 동일

### 3. 포트 — CommentCommandRepository
**신규**: `domain/ma-domain-core/.../community/domain/port/CommentCommandRepository.kt`
- `fun save(newComment: NewComment): Long`

### 4. 포트 수정 — PostQueryRepository
**수정**: `domain/ma-domain-core/.../community/domain/port/PostQueryRepository.kt`
- `fun findOne(id: Long): Post` 추가 (non-null, 없으면 Repository에서 예외)

### 5. 포트 수정 — PostCommandRepository
**수정**: `domain/ma-domain-core/.../community/domain/port/PostCommandRepository.kt`
- `fun incrementComments(id: Long)` 추가

### 6. 서비스 — CommentCommandService
**신규**: `domain/ma-domain-core/.../community/application/CommentCommandService.kt`
- `PostCommandService.create()` 패턴 따름
- `memberQueryRepository.findOne(email)` → 닉네임 획득
- `postQueryRepository.findOne(postId)` → 게시글 존재 확인
- `commentCommandRepository.save(newComment)` → 댓글 저장
- `postCommandRepository.incrementComments(postId)` → 카운트 증가
- Service는 조합만 담당 (규칙 11)

### 7. 인프라 — CommentTable
**신규**: `infrastructure/storage/ma-db-core/.../community/entity/table/CommentTable.kt`
- `BaseTable("COMMUNITY_COMMENTS", "COMMUNITY_COMMENT_ID")` 상속
- `PostTable` 패턴과 동일

### 8. 인프라 — CommentEntity
**신규**: `infrastructure/storage/ma-db-core/.../community/entity/CommentEntity.kt`
- `from(ResultRow)` 팩토리 메서드
- `toDomain()`은 조회 기능 구현 시 추가 (현재 불필요)

### 9. 인프라 — CommentCommandDao
**신규**: `infrastructure/storage/ma-db-core/.../community/dao/CommentCommandDao.kt`
- `PostCommandDao.save()` 패턴과 동일

### 10. 인프라 — CommentCommandCoreRepository
**신규**: `infrastructure/storage/ma-db-core/.../community/repository/CommentCommandCoreRepository.kt`

### 11. 인프라 수정 — PostQueryDao
**수정**: `infrastructure/storage/ma-db-core/.../community/dao/PostQueryDao.kt`
- `fun findOne(id: Long): PostEntity?` 추가
- `deleted eq false` 조건 포함

### 12. 인프라 수정 — PostQueryCoreRepository
**수정**: `infrastructure/storage/ma-db-core/.../community/repository/PostQueryCoreRepository.kt`
- `findOne` 구현: `dao.findOne(id)?.toDomain() ?: throw EntityNotFoundException(...)`

### 13. 인프라 수정 — PostCommandDao
**수정**: `infrastructure/storage/ma-db-core/.../community/dao/PostCommandDao.kt`
- `fun incrementComments(id: Long)` 추가
- `PostTable.update({ PostTable.id eq id }) { with(SqlExpressionBuilder) { it[comments] = comments + 1 } }`

### 14. 인프라 수정 — PostCommandCoreRepository
**수정**: `infrastructure/storage/ma-db-core/.../community/repository/PostCommandCoreRepository.kt`
- `incrementComments` 위임

### 15. ValidationMessages 수정
**수정**: `boot/ma-boot-web/.../support/validation/ValidationPatterns.kt`
- `COMMENT_CONTENT_REQUIRED = "댓글 내용은 필수입니다."` 추가

### 16. API 요청/응답 DTO
**신규**: `boot/ma-boot-web/.../community/api/request/NewCommentRequest.kt`
- `@field:NotBlank(message = ValidationMessages.COMMENT_CONTENT_REQUIRED) val content: String?`

**신규**: `boot/ma-boot-web/.../community/api/response/NewCommentResponse.kt`
- `val commentId: Long`

### 17. 컨트롤러
**신규**: `boot/ma-boot-web/.../community/api/CommunityCommentCommandApi.kt`
- `@RequestMapping("/api/community/posts/{postId}/comments")`
- `@PostMapping` + `@ResponseStatus(HttpStatus.CREATED)`
- `@AuthenticationPrincipal email`, `@PathVariable postId`, `@Valid @RequestBody request`

### 18. 테스트
- **NewCommentTest**: 유효성 검증 (빈 내용, 500자 초과)
- **CommentCommandServiceTest**: mockk 기반 조합 검증
- **CommunityCommentCommandApiTest**: REST Docs 문서화

### 19. REST Docs
- **CommunityVocabulary.kt**: 댓글 관련 vocabulary 추가
- **AsciiDoc**: `community/create-comment.adoc` 생성
- **main.adoc**: 연결 추가

## 핵심 규칙 적용
- **규칙 5 (변경됨)**: 포트는 `List<DomainObject>` 반환, 일급 컬렉션은 포트에서 사용하지 않음
- **규칙 11**: Service는 조합만 담당
- **규칙 14**: `findOne(id)` 메서드명 (파라미터로 유추 가능)
- **규칙 15**: RESTful URL — `POST /api/community/posts/{postId}/comments`

## 검증
1. `./gradlew compileKotlin` — 컴파일 확인
2. `./gradlew test` — 전체 테스트 통과
3. REST Docs snippet 생성 확인
