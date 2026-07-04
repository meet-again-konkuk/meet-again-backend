# Plan: 커뮤니티 잔여 요청 REQ-011~014 (조회 상태필드 · 게시글 수정/삭제 · 신고/차단 · 이미지)

- 작성일: 2026-07-03
- 작업 유형: 기능 개발
- 대상 저장소: meet-again-backend (현재 브랜치 develop)
- 스펙 단일 소스: 프론트 `origin/develop:docs/to-backend.md` 의 REQ-011 / REQ-012 / REQ-013 / REQ-014

---

## 개요

프론트 to-backend.md 의 커뮤니티 잔여 요청 4건을 **4개의 독립 PR(=Phase)** 로 나눠 구현한다.
Phase 번호와 REQ 번호는 의도적으로 다르다(의존성·리스크 순서로 재배열).

| Phase | REQ | 내용 | 우선순위 | DB 마이그레이션 | 핵심 의존성 |
|-------|-----|------|----------|-----------------|-------------|
| **Phase 1** | REQ-011 | 조회 응답에 `likedByMe`·`isMine`·`commentCount` 추가 | high | 불필요 | 없음 (기존 like/author 관계 재사용) |
| **Phase 2** | REQ-012 | 본인 게시글 수정(PATCH)·삭제(DELETE, soft) | high | 불필요 (기존 컬럼 재사용) | Phase 1 무관, 단독 가능 |
| **Phase 3** | REQ-014 | 신고·차단 + 목록/상세 차단 필터 | high | **신규 테이블 2개** | Phase 1의 `Viewer`/조회 경로 확장 |
| **Phase 4** | REQ-013 | 게시글 이미지 1장 첨부/교체/삭제 + 조회 필드 | medium | **신규 테이블 1개** | Phase 2의 `Post.validateOwnership`, Phase 1의 조회 응답 |

### 아키텍처 컨텍스트 (기존 패턴 확정)

- **레이어**: `boot/ma-boot-web`(api) → `domain/ma-domain-core`(application/domain/port) ← `infrastructure/storage/ma-db-core`(dao/entity/table/repository) 구현. 배치는 `boot/ma-boot-batch`.
- **포트 배치**: community 포트는 `domain/community/domain/port` **단일 평면** 유지. 신규 모델은 하위 패키지(`report`, `block`, `image`)에 둔다 (xroom media 서브패키지 선례).
- **ID 식별자**: community는 **plain Long** 사용 (기존 PostResponse/CommentResponse 가 `@EncryptId` 미사용, 스펙 예시도 `101`/`301`/`401`/`501` 숫자). 신규 응답의 `mediaId`/`reportId`/`blockId` 도 plain Long 유지. → xroom media 의 `@EncryptId(MEDIA)` 패턴은 community 에 적용하지 않음.
- **인증**: `@LoginMember memberInfo: MemberInfo` (`memberInfo.id`). 현재 조회 API는 인증 파라미터를 안 받으므로 REQ-011 에서 추가한다. 커뮤니티 전 경로 `anyRequest().authenticated()`.
- **소프트 삭제/감사**: 모든 테이블은 `BaseTable` 상속 → `deleted`/`deletedDate`/`deletedBy` + `createdDate`/`lastModifiedDate` 자동. `activeRows { }` / `softDelete(where, auditUser)` 헬퍼 제공.
- **검증/오케스트레이션 규칙**: 상태·소유권 판단은 도메인 객체 메서드(`Comment.validateOwnership` 선례) 또는 repo 조회가 필요한 사전검증은 `@Component` Validator(`CommentValidator`/`XroomValidator` 선례). application Service는 flat 위임만, private 헬퍼 금지.
- **요청 DTO → 도메인**: Request는 raw 값만 받고 VO 생성은 도메인(`NewPost`/`PostDetails` 등)이 담당.

### 예외 → HTTP 상태 매핑 (기존 GlobalExceptionHandler 기준)

| 도메인 예외 | HTTP | 용도 (본 계획) |
|-------------|------|----------------|
| `EntityNotFoundException` | 404 | 게시글/댓글/블록 없음·soft-deleted |
| `AccessDeniedException` | 403 | 작성자/블록 소유자 아님 |
| `DuplicateException` | 409 | 동일 대상 중복 신고 |
| `InvalidValueException` / `InvalidStateException` | 400 | 본인 신고/차단, reason·detail 검증 실패, 지원하지 않는 확장자 |
| `MaxUploadSizeExceededException` | 400 (`FILE_SIZE_EXCEEDED`) | 10MB 초과 (multipart 설정에서 이미 처리) |

> 스펙의 413/415/422 는 전용 매핑을 추가하지 않고 xroom 선례대로 400 계열로 수렴한다(**결정 D3**). 매직바이트 불일치도 400(`InvalidValueException`). 프론트 회신 시 "상태코드 대신 에러코드로 분기" 명시 필요.

---

## Phase 1 — REQ-011: 조회 응답 사용자 상태 필드

### 목표
게시글 목록/상세, 루트 댓글 상세 응답에 `likedByMe`(내 활성 좋아요 여부), `isMine`(작성자 == 인증회원), `commentCount`(목록 전용, soft-deleted 제외 루트+대댓글 합)를 **additive** 하게 추가한다. **N+1 금지**.

### 조회별 필드 매트릭스

| 응답 | likedByMe | isMine | commentCount |
|------|-----------|--------|--------------|
| 게시글 목록 `data[]` | O | O | O |
| 게시글 상세(post) | O | O | — (comments 배열로 프론트 계산) |
| 게시글 상세 comments[]/replies[] | O | O | — |
| 루트 댓글 상세(comment)/replies[] | O | O | — |

### 변경 파일

| 파일 | 변경 | 내용 |
|------|------|------|
| `domain/community/domain/LikedIds.kt` | 신규 | 일급 컬렉션 `LikedIds(val data: Set<Long>)`, `contains(id): Boolean` |
| `domain/community/domain/Viewer.kt` | 신규 | `Viewer(viewerId, likedIds)`(private props) — `isLikedByMe(targetId): Boolean`, `isMine(authorId): Boolean` |
| `domain/community/domain/CommentCounts.kt` | 신규 | 일급 컬렉션 `CommentCounts(val data: Map<Long,Int>)`, `countOf(postId): Int`, `companion from(...)` (LikeCounts 미러) |
| `domain/community/domain/PostWithAuthor.kt` | 수정 | 필드 `likedByMe`·`isMine`·`commentCount` 추가 |
| `domain/community/domain/CommentWithAuthor.kt` | 수정 | 필드 `likedByMe`·`isMine` 추가 |
| `domain/community/domain/ReplyWithAuthor.kt` | 수정 | 필드 `likedByMe`·`isMine` 추가 |
| `domain/community/domain/Posts.kt` | 수정 | `combineWithAuthors(members, likeCounts, commentCounts, viewer)` 로 시그니처 확장 |
| `domain/community/domain/Comments.kt` | 수정 | `groupByRootComment(members, likeCounts, viewer)` |
| `domain/community/domain/CommentDetail.kt` | 수정 | `combineWithAuthor(members, likeCounts, viewer)` |
| `domain/community/domain/Replies.kt` | 수정 | `combineWithAuthors(members, likeCounts, viewer)` |
| `domain/community/domain/CommentWithPreviewReplies.kt` | 수정 | `combineWithAuthor(members, likeCounts, viewer)` |
| `domain/community/domain/CommentsWithPreviewReplies.kt` | 수정 | `combineWithAuthors(members, likeCounts, viewer)` |
| `domain/community/domain/port/PostLikeRepository.kt` | 수정 | `findLikedPostIds(memberId: Long, postIds: List<Long>): Set<Long>` 추가 |
| `domain/community/domain/port/CommentLikeRepository.kt` | 수정 | `findLikedCommentIds(memberId: Long, commentIds: List<Long>): Set<Long>` 추가 |
| `domain/community/domain/port/CommentQueryRepository.kt` | 수정 | `count(postIds: List<Long>): Map<Long,Int>` 추가 (soft-deleted 제외, PostLikeRepository.count와 네이밍 통일) |
| `domain/community/application/PostQueryService.kt` | 수정 | `find(category, cursor, viewerId)`·`findDetail(id, viewerId)` — 좋아요집합/댓글수/Viewer 조립 |
| `domain/community/application/CommentQueryService.kt` | 수정 | `findDetail(commentId, viewerId)` |
| `ma-db-core/community/dao/PostLikeDao.kt` | 수정 | `findLikedPostIds(memberId, postIds)` (select postId, `deleted eq false and memberId eq and postId inList`) |
| `ma-db-core/community/dao/CommentLikeDao.kt` | 수정 | `findLikedCommentIds(memberId, commentIds)` |
| `ma-db-core/community/dao/CommentQueryDao.kt` | 수정 | `count(postIds)` (`deleted eq false and postId inList`, groupBy postId) |
| `ma-db-core/community/repository/PostLikeCoreRepository.kt` | 수정 | 신규 포트 메서드 위임 |
| `ma-db-core/community/repository/CommentLikeCoreRepository.kt` | 수정 | 위임 |
| `ma-db-core/community/repository/CommentQueryCoreRepository.kt` | 수정 | 위임 |
| `web/community/api/PostQueryApi.kt` | 수정 | `findPosts`·`findDetail` 에 `@LoginMember memberInfo` 추가, `memberInfo.id` 전달 |
| `web/community/api/CommentQueryApi.kt` | 수정 | `findDetail` 에 `@LoginMember` 추가 |
| `web/community/api/response/PostResponse.kt` | 수정 | `likedByMe`·`isMine`·`commentCount` 필드 + `from` 매핑 |
| `web/community/api/response/PostDetailResponse.kt` | 수정 | `likedByMe`·`isMine` 필드 |
| `web/community/api/response/CommentResponse.kt` | 수정 | `likedByMe`·`isMine` 필드 |
| `web/community/api/response/ReplyResponse.kt` | 수정 | `likedByMe`·`isMine` 필드 |

### N+1 방지 쿼리 설계 (목록 30개 기준)

`PostQueryService.find` 흐름 — 게시글 수와 무관하게 **고정 4~5 쿼리**:
1. `postQueryRepository.find(...)` — 페이지 조회 (1)
2. `memberQueryRepository.findByIds(authorIds)` — 작성자 일괄 (1)
3. `postLikeRepository.count(postIds)` — 좋아요 수 grouped (1)
4. `postLikeRepository.findLikedPostIds(viewerId, postIds)` — 내가 좋아요한 postId 집합 (1)
5. `commentQueryRepository.count(postIds)` — 댓글 수 grouped (1)

`isMine` 은 이미 조회된 `post.authorId` 와 `viewerId` 메모리 비교(`viewer.isMine`). 상세/댓글상세도 동일하게 응답 포함 comment IDs 를 모아 좋아요 수/내 좋아요 여부 일괄 조회.

### 구현 순서
1. 도메인 VO 신규: `LikedIds`, `Viewer`, `CommentCounts`
2. `PostWithAuthor`/`CommentWithAuthor`/`ReplyWithAuthor` 필드 확장
3. combine 계열 시그니처 확장 (Posts→Comments→CommentDetail→Replies→...)
4. 포트 3개 메서드 추가 → DAO 구현 → CoreRepository 위임
5. Service 조립 로직 수정
6. API `@LoginMember` 추가 + 응답 DTO 매핑
7. 테스트

### 테스트 계획 (Phase 1)
- **도메인 단위**: `Viewer.isMine`/`isLikedByMe`, `LikedIds.contains`, `CommentCounts.countOf`. (Post/Comment.isWrittenBy는 리뷰에서 미배선 중복으로 판정, 미도입 — 작성자 판단은 Viewer.isMine 단일화)
- **Service(E2E, DB 헬퍼)**: 좋아요 후 목록/상세에 `likedByMe=true`; 취소 후 `false`+감소 count; 본인/타인 `isMine` 분기; `commentCount` = 상세의 soft-deleted 제외 루트+대댓글 수 일치; 삭제된 댓글 placeholder 도 `isMine` 유지.
- **쿼리 수 회귀**: 게시글 5개 vs 30개 목록에서 쿼리 수 동일함 확인 (N+1 미발생).
- **REST Docs**: 세 조회 API 스니펫에 신규 필드 반영.

### PR 경계
DB 스키마 변화 없음. 순수 응답 필드 additive. 프론트는 optional 파싱 준비됨 → 단독 배포 안전.

---

## Phase 2 — REQ-012: 본인 게시글 수정/삭제

### 목표
작성자 본인만 게시글을 수정(PATCH, 전체 필드)·삭제(DELETE, soft delete)한다. 404(없음/삭제됨) / 403(존재하나 타인) 구분. 삭제 게시글은 목록 제외·상세 404·신규 댓글/좋아요 차단.

### 변경 파일

| 파일 | 변경 | 내용 |
|------|------|------|
| `domain/community/domain/PostDetails.kt` | 신규 | VO `PostDetails(category, title, content)` — 제목/내용 blank·길이 검증 + 상수(`MAX_TITLE_LENGTH`/`MAX_CONTENT_LENGTH`) 이관 |
| `domain/community/domain/NewPost.kt` | 수정 | `NewPost(authorId, details: PostDetails)` 로 조립 (검증 단일화). 상수/검증은 `PostDetails` 로 이동 |
| `domain/community/domain/Post.kt` | 수정 | `validateOwnership(memberId: Long)` 추가 (authorId != memberId → `AccessDeniedException(COMMUNITY_POST)`) — Comment 선례 미러 |
| `domain/community/domain/port/PostCommandRepository.kt` | 수정 | `update(postId: Long, details: PostDetails)`·`softDelete(postId: Long, memberId: Long)` 추가 |
| `domain/community/application/PostCommandService.kt` | 수정 | `create(newPost)` 는 `PostDetails` 조립 반영, `update(postId, memberId, details)`·`delete(postId, memberId)` 추가 |
| `domain/community/application/PostLikeService.kt` | 수정 | `like` 시 게시글 존재(active) 검증 추가 → 삭제글 좋아요 404 |
| `ma-db-core/community/dao/PostCommandDao.kt` | 수정 | `update(postId, details)`(category/title/content + lastModified) · `softDelete(postId, memberId)`(BaseTable.softDelete) |
| `ma-db-core/community/repository/PostCommandCoreRepository.kt` | 수정 | 신규 포트 위임 |
| `web/community/api/request/NewPostRequest.kt` | 수정 | `toNewPost` → `PostDetails` 경유 조립 |
| `web/community/api/request/EditPostRequest.kt` | 신규 | raw `category`/`title`/`content` + `@NotBlank`/`@Size`, `toPostDetails()` |
| `web/community/api/response/UpdatePostResponse.kt` | 신규 | `UpdatePostResponse(postId, updated=true)` |
| `web/community/api/PostCommandApi.kt` | 수정 | `PATCH /{postId}` update, `DELETE /{postId}` delete(204) 추가 |

### 서비스 시그니처 (application, flat 위임)
- `update(postId: Long, memberId: Long, details: PostDetails)`:
  `post = postQueryRepository.findOne(postId)`(404) → `post.validateOwnership(memberId)`(403) → `postCommandRepository.update(postId, details)`
- `delete(postId: Long, memberId: Long)`:
  `post = postQueryRepository.findOne(postId)`(404) → `post.validateOwnership(memberId)`(403) → `postCommandRepository.softDelete(postId, memberId)`
  > Phase 4 에서 이 delete 에 `postImageCommandRepository.softDeleteByPost(postId, memberId)` 한 줄 추가 (연쇄 media soft delete).

### 삭제 정책 정합성
- **목록 제외**: `PostQueryDao.find` 는 이미 `deleted eq false` 필터 → 자동 제외.
- **상세 404**: `findOne` 은 `activeRows` → soft-deleted 는 404.
- **신규 댓글 차단**: `CommentValidator.validatePostExists` 가 `exists`(deleted 필터) 사용 → 자동 404.
- **신규 좋아요 차단**: `PostLikeService.like` 에 **존재 검증 신규 추가**(현재 무검증). 삭제글/없는글 좋아요 시 404.
- **반복 삭제**: soft-deleted 는 `findOne` 404 → 재삭제 404 (스펙 권장 일치).
- **탈퇴/익명화 충돌 없음**: `MemberDataCleaner.cleanCommunity` 는 게시글을 **삭제하지 않고 authorId(비PII) 보존**(조회 시 "알 수 없음"). 본 soft delete 는 작성자 본인 행위 → 정책 독립, 충돌 없음.

### 응답 코드 결정
- PATCH → `200` `{ "postId", "updated": true }`
- DELETE → `204 No Content` (스펙 권장). `@ResponseStatus(HttpStatus.NO_CONTENT)`.

### 테스트 계획 (Phase 2)
- **도메인**: `PostDetails` 검증(blank/length), `Post.validateOwnership`(본인 통과/타인 403).
- **Service E2E**: 수정 후 좋아요·댓글 보존한 채 최신 내용; 타인 수정/삭제 403; 없는/삭제된 글 404; 삭제 후 목록 제외·상세 404·신규 댓글/좋아요 404; 반복 삭제 404.
- **REST Docs**: PATCH/DELETE 스니펫(400/403/404 케이스 포함).

### PR 경계
스키마 변화 없음(기존 BaseTable 컬럼 재사용). Phase 1 과 독립적이나 프론트는 REQ-011 `isMine` 으로 메뉴 노출을 결정하므로 **Phase 1 배포 후 활성화 권장**.

---

## Phase 3 — REQ-014: 신고 · 차단

### 목표
게시글/댓글 신고(중복 409·본인 400), 콘텐츠 작성자 차단(idempotent), 차단 목록/해제. 차단은 단방향, 목록은 **DB 쿼리 단계에서 제외**(cursor/page size 안정), 차단 게시글 직접 조회 404, 차단 댓글은 placeholder+`blockedAuthor:true`.

### 신규 테이블 DDL (FK 금지 — PK·INDEX 만)

**`COMMUNITY_REPORTS`** (PK `COMMUNITY_REPORT_ID`, BaseTable 상속)

| 컬럼 | 타입 | 비고 |
|------|------|------|
| REPORTER_ID | BIGINT | 신고자 |
| TARGET_TYPE | VARCHAR(20) | `POST` / `COMMENT` |
| TARGET_ID | BIGINT | 대상 게시글/댓글 id |
| TARGET_AUTHOR_ID | BIGINT | 대상 작성자 (본인 신고 판별·운영 참조) |
| TARGET_TITLE | VARCHAR(255) NULL | **신고 시점 스냅샷** (게시글 title, 댓글은 NULL) |
| TARGET_CONTENT | TEXT | **신고 시점 원문 스냅샷** — 신고 후 수정/삭제돼도 증거 보존 (결정 D5) |
| REASON | VARCHAR(20) | enum |
| DETAIL | VARCHAR(500) NULL | optional |
| STATUS | VARCHAR(20) | `RECEIVED`/`REVIEWING`/`ACTIONED`/`DISMISSED` |

- INDEX `(REPORTER_ID, TARGET_TYPE, TARGET_ID)` — 중복 신고 판정.
- INDEX `(TARGET_TYPE, TARGET_ID)` — 운영 조회.

**`COMMUNITY_BLOCKS`** (PK `COMMUNITY_BLOCK_ID`, BaseTable 상속)

| 컬럼 | 타입 | 비고 |
|------|------|------|
| BLOCKER_ID | BIGINT | 차단한 회원 |
| BLOCKED_ID | BIGINT | 차단당한 회원 |

- INDEX `(BLOCKER_ID, BLOCKED_ID)` — 중복/idempotent 판정.
- INDEX `(BLOCKER_ID)` — 목록 조회 + 차단집합 조회.

### 변경 파일 — 도메인 (`domain/community/domain/{report,block}` 서브패키지)

| 파일 | 변경 | 내용 |
|------|------|------|
| `domain/community/domain/report/ReportReason.kt` | 신규 | enum SPAM/HARASSMENT/HATE/SEXUAL_CONTENT/PRIVACY/OTHER |
| `domain/community/domain/report/ReportStatus.kt` | 신규 | enum RECEIVED/REVIEWING/ACTIONED/DISMISSED |
| `domain/community/domain/report/ReportTargetType.kt` | 신규 | enum POST/COMMENT |
| `domain/community/domain/report/Report.kt` | 신규 | 엔티티(id, reporterId, targetType, targetId, targetAuthorId, targetTitle?, targetContent, reason, detail?, status, createdDate) |
| `domain/community/domain/report/NewReport.kt` | 신규 | init 검증: detail ≤ 500, **본인 신고**(`reporterId == targetAuthorId` → `InvalidValueException` 400). 기본 status=RECEIVED. **신고 시점 원문 스냅샷(targetTitle?/targetContent) 포함** (결정 D5) |
| `domain/community/domain/report/ReportValidator.kt` | 신규 | `@Component` — 중복 신고(active 존재 시 `DuplicateException` 409). repo 조회 필요 검증 |
| `domain/community/domain/block/Block.kt` | 신규 | 엔티티(id, blockerId, blockedId, createdDate). `validateOwnership(memberId)`(403) |
| `domain/community/domain/block/NewBlock.kt` | 신규 | init 검증: **본인 차단**(`blockerId == blockedId` → `InvalidValueException` 400) |
| `domain/community/domain/block/Blocks.kt` | 신규 | 일급 컬렉션 `Blocks(val data: List<Block>)` — `extractBlockedIds()`, 목록 조립 |
| `domain/community/domain/block/BlockedMemberIds.kt` | 신규 | VO `BlockedMemberIds(val data: Set<Long>)` — `contains(authorId): Boolean` (필터·placeholder 판단) |
| `domain/community/domain/Viewer.kt` | 수정 | Phase 1 `Viewer` 에 `blockedMemberIds` 추가 → `hasBlocked(authorId): Boolean` |
| `domain/community/domain/CommentWithAuthor.kt` | 수정 | `blockedAuthor: Boolean` + placeholder 노출 판단(`displayContent()`가 blocked 시 `"차단한 사용자의 댓글입니다."`) |
| `domain/community/domain/ReplyWithAuthor.kt` | 수정 | `blockedAuthor: Boolean` + placeholder |
| `domain/community/domain/EntityType (공용)` | 수정 | `EntityType` 에 `COMMUNITY_REPORT`, `COMMUNITY_BLOCK` 추가 (`domain/ma-domain-core/.../exception/EntityType.kt`) |

### 변경 파일 — 포트 (flat `domain/community/domain/port`)

| 파일 | 내용 (시그니처) |
|------|-----------------|
| `port/ReportCommandRepository.kt` (신규) | `save(newReport: NewReport): Long` |
| `port/ReportQueryRepository.kt` (신규) | `existsActive(reporterId: Long, targetType: ReportTargetType, targetId: Long): Boolean` |
| `port/BlockCommandRepository.kt` (신규) | `save(newBlock: NewBlock): Long`; `softDelete(blockId: Long, blockerId: Long)` |
| `port/BlockQueryRepository.kt` (신규) | `findByBlocker(blockerId): List<Block>`; `findBlockedMemberIds(blockerId): Set<Long>`; `findExistingActive(blockerId, blockedId): Block?`; `findOne(blockId): Block` (404) |

### 변경 파일 — 애플리케이션 서비스

| 파일 | 내용 |
|------|------|
| `application/ReportCommandService.kt` (신규) | `reportPost(postId, reporterId, reason, detail): ReportResult` / `reportComment(commentId, reporterId, reason, detail)`. 흐름: 대상 findOne(404) → targetAuthorId·**원문 스냅샷(title/content) 확보** → `NewReport`(본인 400) → `reportValidator`(중복 409) → save |
| `application/BlockCommandService.kt` (신규) | `blockPostAuthor(postId, blockerId): BlockResult` / `blockCommentAuthor(...)`. 흐름: 대상 findOne(404) → blockedId=작성자 → `NewBlock`(본인 400) → 기존 active 있으면 그대로(200), 없으면 save(201) → blockedNickname 조회. `unblock(blockId, blockerId)`: findOne(404)→validateOwnership(403)→softDelete |
| `application/BlockQueryService.kt` (신규) | `findBlocks(blockerId): List<BlockView>` — `Blocks` + `Members` 닉네임 조립 |
| `application/result/ReportResult.kt`·`BlockResult.kt`·`BlockView.kt` (신규) | reportId/status · (blockId, blockedNickname, newlyBlocked) · (blockId, nickname, blockedAt) |

### 변경 파일 — 차단 필터 적용 (Phase 1 조회 경로 확장)

| 파일 | 변경 |
|------|------|
| `application/PostQueryService.kt` | `find`/`findDetail` 진입 시 `blockQueryRepository.findBlockedMemberIds(viewerId)` 1회 조회 → 목록은 DAO 에 제외 id 전달, 상세는 `viewer.hasBlocked(post.authorId)` 시 404, comments 는 `Viewer` 로 placeholder |
| `application/CommentQueryService.kt` | `findDetail` 에 blockedIds 반영한 `Viewer` 조립 (placeholder) |
| `domain/community/domain/port/PostQueryRepository.kt` | `find(category, cursor, excludedAuthorIds: Set<Long>)` 로 확장 |
| `ma-db-core/community/dao/PostQueryDao.kt` | `find` 에 `authorId notInList excludedAuthorIds` 조건 추가 (pagination 前 DB 단계) |
| `ma-db-core/community/repository/PostQueryCoreRepository.kt` | 시그니처 위임 |

### 변경 파일 — DB (신규 테이블)

| 파일 | 변경 |
|------|------|
| `ma-db-core/community/entity/table/ReportTable.kt` | 신규 (위 DDL) |
| `ma-db-core/community/entity/table/BlockTable.kt` | 신규 (위 DDL) |
| `ma-db-core/community/entity/{ReportEntity,BlockEntity}.kt` | 신규 (`toDomain`, `from(row)`) |
| `ma-db-core/community/dao/{ReportCommandDao,ReportQueryDao,BlockCommandDao,BlockQueryDao}.kt` | 신규 |
| `ma-db-core/community/repository/{ReportCommandCoreRepository,ReportQueryCoreRepository,BlockCommandCoreRepository,BlockQueryCoreRepository}.kt` | 신규 |

### 변경 파일 — API (web)

| 파일 | 변경 |
|------|------|
| `web/community/api/ReportApi.kt` (신규) | `POST /api/community/posts/{postId}/reports`(201) · `POST /api/community/comments/{commentId}/reports`(201) |
| `web/community/api/BlockApi.kt` (신규) | `POST /api/community/posts/{postId}/author/block`(201/200) · `POST /api/community/comments/{commentId}/author/block`(201/200) · `GET /api/community/blocks`(200) · `DELETE /api/community/blocks/{blockId}`(204) |
| `web/community/api/request/NewReportRequest.kt` (신규) | `reason: ReportReason`, `detail: String?`(`@Size(max=500)`) |
| `web/community/api/response/{ReportResponse,BlockResponse,BlocksResponse,BlockItemResponse}.kt` (신규) | 스펙 JSON 형태 |

### 차단 가시성 정책 요약
- 게시글 목록: 차단 작성자 게시글을 **DB 쿼리에서 제외** → page size/cursor 안정.
- 게시글 상세: 차단 작성자 게시글 직접 조회 → **404** (정보 노출 최소화).
- 댓글/답글: 차단 작성자 것은 내용 대신 `"차단한 사용자의 댓글입니다."` + `blockedAuthor:true`. placeholder 에는 좋아요/답글/신고 액션 미노출(프론트 처리, 서버는 blockedAuthor 플래그 제공).
- N+1 방지: 인증회원 blocked member id 집합 1회 조회 후 메모리 판단.
- idempotent: 재차단 200(동일 blockId 반환), 최초 201.
- 신고 ≠ 차단(독립).

### 구현 순서
1. `EntityType` 확장 → enum 3종(report) + block 도메인/VO
2. 포트 4개 → 테이블 2개 + entity/dao/coreRepository
3. `ReportCommandService`/`BlockCommandService`/`BlockQueryService` + result
4. 조회 경로 차단 필터 확장 (Viewer.hasBlocked, PostQueryRepository.find 제외 id)
5. API 4엔드포인트 + request/response
6. 테스트

### 테스트 계획 (Phase 3)
- **도메인**: `NewReport` 본인신고 400, `NewBlock` 본인차단 400, `Block.validateOwnership` 403, `BlockedMemberIds.contains`, `Viewer.hasBlocked`.
- **Service E2E**: 게시글/댓글/답글 신고가 target type/id·작성자 정확 저장; 중복 신고 409; 본인 신고/차단 400; 차단 후 목록에서 대상 게시글 제외 + cursor/page size 유지; 상세 404; 댓글 placeholder+blockedAuthor; 차단 해제 후 재노출; 타 계정에서도 동일 상태(서버 기준).
- **REST Docs**: report/block 4엔드포인트 + 400/403/404/409.

### PR 경계
신규 테이블 2개 → DDL 반영 필요. 조회 경로(Phase 1) 확장 포함 → **Phase 1 이후 진행**. 관리자 신고 처리 API/화면은 범위 밖 — status 는 RECEIVED 저장만, 변경 주체·자동숨김 threshold 는 운영 정의로 이연(결정 D5).

---

## Phase 4 — REQ-013: 게시글 이미지 1장 (첨부/교체/삭제)

### 목표
게시글당 active 이미지 최대 1장. 2단계 흐름(게시글 생성 후 `postId` 로 업로드). 조회 응답에 `imageUrl`/`thumbnailUrl`(없으면 null). 교체 시 기존 soft delete, 물리 파일은 cleanup job 으로 정리. **xroom media/FileStorage 인프라 최대 재사용.**

### 재사용 지점 (그대로 사용)
| 재사용 자산 | 위치 | 용도 |
|-------------|------|------|
| `FileStorage`(port) + `LocalFileStorage`/`S3FileStorage` | `common/domain/file/port` + `ma-file-storage` | 파일 저장/`deleteByKey` |
| `FileUrlResolver`(port) + `Local`/`S3FileUrlResolver` | 동일 | storageKey → URL (local prefix / S3 presigned) |
| `ThumbnailGenerator` + `ThumbnailatorThumbnailGenerator` | 동일 | 썸네일(400px, best-effort null) |
| `PhotoFile` VO | `common/domain/file` | **10MB·비어있음 검증 내장** |
| `AllowedExtension` | 동일 | 확장자 화이트리스트 (단, svg 포함 → community 는 svg 배제 필요, 아래 참조) |
| `StoragePath`/`StorageDomainType`/`StorageUsageType` | 동일 | 경로 규칙 |
| `MediaPurgePolicy.purgeCutoff` (7일) | `xroom/domain/media/policy` | 재사용 or 동형 신설 |
| `AbstractJobConfig`/`DateJobParameter` | `job/common` | 배치 스캐폴딩 |

> **패턴 미러(신설 필요):** `MediaProcessor`/`MediaUrlResolver`/`MediaPurger`/`Media`/`MemoryMediaTable` 는 `memoryId` 키에 묶여 xroom 전용 → community 는 `postId` 키의 **평행 클래스**를 신설한다(member profile photo 가 동일한 평행 선례). 배치 purge job 도 테이블-특정이라 평행 신설.

### 신규 테이블 DDL — `COMMUNITY_POST_IMAGES` (PK `COMMUNITY_POST_IMAGE_ID`, BaseTable)

| 컬럼 | 타입 | 비고 |
|------|------|------|
| POST_ID | BIGINT | INDEX `(POST_ID)` |
| STORAGE_KEY | VARCHAR(512) | |
| ORIGINAL_FILENAME | VARCHAR(255) | |
| MIME_TYPE | VARCHAR(100) | |
| FILE_SIZE | BIGINT | |
| THUMBNAIL_KEY | VARCHAR(512) NULL | 썸네일 실패 시 null |

- FK 없음. INDEX `(POST_ID)`. purge 는 BaseTable `deleted`/`deletedDate` 사용.
- width/height 컬럼 없음 — 치수 저장·EXIF 제거·디코딩 검증은 후속 PR로 이연, 이번 범위는 **확장자 + 매직바이트 검증**까지(결정 D2).

### 변경 파일 — 공용 file 설정

| 파일 | 변경 |
|------|------|
| `common/domain/file/StorageDomainType.kt` | `COMMUNITY("community")` 추가 |
| `common/domain/file/StorageUsageType.kt` | `POST_IMAGE("post-image")` 추가 (THUMBNAIL 재사용) |
| `common/domain/file/ImageSignature.kt` | **신규** — jpg/jpeg(`FF D8 FF`)·png(`89 50 4E 47`)·webp(`RIFF....WEBP`) 매직바이트 정의 enum. `matches(headerBytes, extension): Boolean` — 확장자와 실제 내용물 서명 일치 검증(결정 D2). xroom 등 타 도메인도 재사용 가능하도록 공용 file 패키지에 배치 |
| `web/config/SecurityConfig.kt` | `GET /files/community/**` permitAll 추가 (local 서빙, xroom `/files/memory/**` 선례 미러 — 결정 D7, 운영은 S3 presigned 로 해소) |

### 변경 파일 — 도메인 (`domain/community/domain/image`)

| 파일 | 내용 |
|------|------|
| `image/PostImage.kt` (신규) | 엔티티(id, postId, storageKey, originalFilename, mimeType, fileSize, thumbnailKey?) |
| `image/NewPostImage.kt` (신규) | `of(postId, photoFile, storageKey, thumbnailKey?)`, `toPostImage(id)` |
| `image/PostImageUrls.kt` (신규) | `(imageUrl, thumbnailUrl?)` |
| `image/PostImageProcessor.kt` (신규) | `@Component(FileStorage, ThumbnailGenerator)` — `process(postId, photoFile): NewPostImage`. `StoragePath.of(COMMUNITY, POST_IMAGE, postId)` 저장 + 썸네일. **community 허용 확장자 검증(jpg/jpeg/png/webp, svg 배제) + 매직바이트 검증(`ImageSignature.matches`, 불일치 시 `InvalidValueException` 400)** 포함 (결정 D2·D3) |
| `image/PostImageUrlResolver.kt` (신규) | `@Component(FileUrlResolver)` — `resolve(image): PostImageUrls`, `resolveByPosts(images): Map<Long,PostImageUrls>` (목록) |
| `image/PostImagePurger.kt` (신규) | `@Component(FileStorage, PostImageCommandRepository)` — `purge(image)`(deleteByKey ×2 + hard delete) |
| `domain/community/domain/port/PostImageCommandRepository.kt` (신규) | `save(newPostImage): Long`; `softDeleteByPost(postId, memberId)`; `delete(imageId)` |
| `domain/community/domain/port/PostImageQueryRepository.kt` (신규) | `findActiveByPost(postId): PostImage?`; `findActiveByPosts(postIds): List<PostImage>`; `findDeletedBefore(cutoff, cursorId, pageSize): List<PostImage>` |

### 변경 파일 — 애플리케이션 / 조회 통합

| 파일 | 변경 |
|------|------|
| `application/PostImageCommandService.kt` (신규) | `upload(postId, memberId, photoFile): PostImageUploadResult` — findOne(404)→`post.validateOwnership`(403, **Phase 2 의존**)→기존 active 조회(201/200 판별)→`softDeleteByPost`(교체)→`processor.process`→save→`resolve`. `delete(postId, memberId)`: findOne(404)→validateOwnership→softDeleteByPost |
| `application/result/PostImageUploadResult.kt` (신규) | `(mediaId, postId, imageUrl, thumbnailUrl?, replaced: Boolean)` |
| `application/PostCommandService.kt` (수정) | `delete` 에 `postImageCommandRepository.softDeleteByPost(postId, memberId)` 추가 (게시글 삭제 시 이미지 연쇄 soft delete — REQ-012 정책) |
| `application/PostQueryService.kt` (수정) | `find`: `postImageQueryRepository.findActiveByPosts(postIds)` → `resolveByPosts` → `PostWithAuthor.imageUrl/thumbnailUrl` (1쿼리, N+1 없음). `findDetail`: `findActiveByPost(postId)` → 상세 imageUrl |
| `domain/community/domain/PostWithAuthor.kt` (수정) | `imageUrl: String?`·`thumbnailUrl: String?` 추가 |
| `domain/community/domain/PostDetail.kt` (수정) | `imageUrl: String?`·`thumbnailUrl: String?` 추가 |

### 변경 파일 — DB / API / 배치

| 파일 | 변경 |
|------|------|
| `ma-db-core/community/entity/table/PostImageTable.kt` (신규) | 위 DDL |
| `ma-db-core/community/entity/PostImageEntity.kt` (신규) | `toDomain`, `from(row)` |
| `ma-db-core/community/dao/{PostImageCommandDao,PostImageQueryDao}.kt` (신규) | MediaCommandDao/MediaQueryDao 미러 (`softDeleteByPost`, `findActiveByPost(s)`, `findDeletedBefore`) |
| `ma-db-core/community/repository/{PostImageCommandCoreRepository,PostImageQueryCoreRepository}.kt` (신규) | 위임 |
| `web/community/api/PostImageApi.kt` (신규) | `POST /api/community/posts/{postId}/image`(multipart `image`, 201/200) · `DELETE /api/community/posts/{postId}/image`(204) |
| `web/community/api/response/PostImageUploadResponse.kt` (신규) | `(mediaId, postId, imageUrl, thumbnailUrl?)` |
| `web/community/api/response/{PostResponse,PostDetailResponse}.kt` (수정) | `imageUrl`/`thumbnailUrl` 필드 추가 (없으면 null) |
| `batch/job/domain/community/DeletedPostImagePurgeJobConfig.kt` (신규) | xroom `DeletedMediaPurgeJobConfig` 미러 |
| `batch/job/domain/community/DeletedPostImageItemReader.kt` (신규) | keyset reader (`findDeletedBefore`) |
| `batch/job/domain/community/PostImagePurgeItemWriter.kt` (신규) | `PostImagePurger.purge` |

### 업로드/교체/삭제 흐름
- 교체 원자성: **새 파일 store 성공 후 기존 soft delete** 순서로 확정(결정 D6) — 스펙 "업로드 실패 시 기존 이미지 유지" 충족. 실패 시 @Transactional 롤백 + 고아 파일은 cleanup job 정리. (xroom 은 선-softDelete 이나 community 는 요구가 다름.)
- 응답 코드: 최초 201 / 교체 200 (`replaced` 플래그 → `ResponseEntity`).
- 조회: 목록 `thumbnailUrl` 우선, 상세 `imageUrl`. 없으면 두 필드 null.

### 테스트 계획 (Phase 4)
- **도메인**: `NewPostImage.of/toPostImage`, community 확장자 검증(svg 거부), `ImageSignature.matches`(형식별 서명 일치/불일치), `PostImageUrlResolver` 매핑.
- **Service E2E**: 작성자만 추가/교체/삭제; 미지원 확장자 거부; **확장자만 위장한 파일(내용물 서명 불일치) 400 거부**; 교체/삭제 후 이전 URL 미반환; 게시글 삭제 시 이미지 연쇄 soft delete + purge 대상; 이미지 없는 기존 게시글 응답 호환(null).
- **배치 통합**: `DeletedMediaPurgeJobIntegrationTest` 미러 — deletedDate 7일 경과 이미지 물리삭제+행 hard delete.
- **REST Docs**: image POST/DELETE + 조회 필드.

### PR 경계
신규 테이블 1개 + 배치 job. Phase 2(`Post.validateOwnership`) · Phase 1(조회 응답) 의존 → **마지막**. 운영 배포 시 S3 모드는 yml(`file.storage.mode=s3`, bucket/region/presign-ttl) 주입 필요(별도 인프라 작업, 코드 무변경).

---

## 결정 사항 (2026-07-04 사용자 확정 — 전 항목 해소)

- **D1. NewPost → PostDetails 리팩터 (Phase 2)**: **리팩터 채택.** `NewPost(authorId, details: PostDetails)` 조립으로 생성/수정 검증 단일화.
- **D2. 이미지 검증 수위 (Phase 4)**: **확장자 + 매직바이트 검증까지 채택.** 공용 `ImageSignature` enum으로 jpg/png/webp 내용물 서명 검사(위장 파일 차단). 디코딩 검증·EXIF GPS 제거·width/height 저장은 후속 PR로 이연.
- **D3. 상태코드 (Phase 4)**: **400 계열 유지.** 413/415/422 전용 매핑 미도입, 기존 에러코드(`FILE_SIZE_EXCEEDED`/`INVALID_INPUT_VALUE`)로 구분. to-backend.md 회신에 "상태코드 대신 에러코드 분기" 명시할 것.
- **D4. 썸네일 정책 (Phase 4)**: **best-effort 채택.** 썸네일 생성 실패 시 원본 업로드는 성공, `thumbnailUrl=null`.
- **D5. 신고 스냅샷 (Phase 3)**: **원문 스냅샷 저장 채택.** 신고 접수 시점의 `targetTitle`(게시글만)/`targetContent`를 COMMUNITY_REPORTS 에 복사 저장 — 신고 후 수정/삭제돼도 증거 보존. status 는 RECEIVED 저장만 하고 변경 주체·자동숨김은 운영 정의로 이연.
- **D6. 이미지 교체 순서 (Phase 4)**: **새 파일 store 성공 후 기존 soft delete.** 업로드 실패 시 기존 이미지 유지(스펙 요구).
- **D7. 미디어 접근 경계 (Phase 4)**: **permitAll 채택(A안).** `/files/community/**` 정적 서빙(UUID 파일명), 운영 전환 시 S3 presigned URL 로 자연 해소. 인증 컨트롤러 경유 미도입.

---

## 구현 시 참조 (규칙 본문 비복제)

- 객체/서비스 구현 규칙: [[code-implementation-rules]]
- 모듈·패키지 배치: [[clean-architecture]]
- 도메인 모델링/경계: [[domain-driven-design]]
- 가독성/네이밍/함수: [[clean-code]]
- 테스트 작성: [[kotest-writing]]
- API 문서화: [[rest-docs-writing]]

## 권장 브랜치 / PR 순서
1. `feat/community-req011-view-state` (Phase 1)
2. `feat/community-req012-post-edit-delete` (Phase 2)
3. `feat/community-req014-report-block` (Phase 3, Phase 1 이후)
4. `feat/community-req013-post-image` (Phase 4, Phase 1·2 이후)
</content>
</invoke>
