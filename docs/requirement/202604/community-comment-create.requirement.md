# Design: POST /api/community/posts/{postId}/comments -- 댓글/대댓글 작성

> 작성일: 2026-04-06
> 상태: Draft

## 1. 설계 개요

커뮤니티 게시글에 댓글을 작성하고, 기존 댓글에 1단계 답글(대댓글)을 달 수 있는 API를 구현한다. `parentCommentId`가 null이면 일반 댓글, 값이 있으면 해당 댓글에 대한 답글로 처리하며, 2단계 이상 중첩은 허용하지 않는다.

## 2. 아키텍처

```
┌───────────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                              │
│                                                               │
│  CommunityCommentCommandApi                                   │
│    POST /api/community/posts/{postId}/comments                │
│    └── @AuthenticationPrincipal email                          │
│    └── @Valid @RequestBody NewCommentRequest                   │
│    └── request.toNewComment(email, postId) → NewComment       │
│    └── commentCommandService.create(newComment) → Long        │
│    └── return NewCommentResponse(commentId)                    │
└──────────────────────────┬────────────────────────────────────┘
                           │ (depends on)
┌──────────────────────────▼────────────────────────────────────┐
│ domain/ma-domain-core                                         │
│                                                               │
│  CommentCommandService                                        │
│    + create(newComment: NewComment): Long                      │
│      1. postQueryRepository.findOne(postId) → 존재 확인        │
│      2. if parentCommentId != null:                            │
│         commentQueryRepository.findOne(parentCommentId)        │
│           → 부모 댓글 존재 확인 + 대댓글 여부 검증              │
│      3. commentCommandRepository.save(newComment) → commentId  │
│      4. postCommandRepository.incrementComments(postId)        │
│                                                               │
│  Port Interfaces:                                             │
│    CommentCommandRepository                                    │
│      + save(newComment: NewComment): Long                      │
│    CommentQueryRepository                                      │
│      + findOne(id: Long): Comment                              │
│    PostQueryRepository (수정)                                   │
│      + findOne(id: Long): Post                                 │
│    PostCommandRepository (수정)                                 │
│      + incrementComments(id: Long)                             │
│                                                               │
│  Domain Models:                                               │
│    NewComment(postId, authorEmail, content, parentCommentId?)   │
│    Comment(id, postId, authorEmail, content, parentCommentId?) │
└──────────────────────────┬────────────────────────────────────┘
                           │ (implements)
┌──────────────────────────▼────────────────────────────────────┐
│ infrastructure/storage/ma-db-core                             │
│                                                               │
│  CommentTable (COMMUNITY_COMMENTS)                            │
│    POST_ID, AUTHOR_EMAIL, CONTENT, PARENT_COMMENT_ID          │
│                                                               │
│  CommentEntity → from(ResultRow) / toDomain()                 │
│  CommentCommandDao → save(NewComment): Long                   │
│  CommentQueryDao → findOne(id: Long): CommentEntity?          │
│  CommentCommandCoreRepository → save 위임                      │
│  CommentQueryCoreRepository → findOne + toDomain + 예외        │
│                                                               │
│  PostQueryDao (수정) → findOne(id: Long): PostEntity?          │
│  PostCommandDao (수정) → incrementComments(id: Long)           │
│  PostQueryCoreRepository (수정) → findOne 구현                  │
│  PostCommandCoreRepository (수정) → incrementComments 위임      │
└───────────────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 DDL -- COMMUNITY_COMMENTS 테이블

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`

기존 `COMMUNITY_POSTS` 테이블 아래에 추가:

```sql
-- COMMUNITY COMMENTS
CREATE TABLE COMMUNITY_COMMENTS
(
    COMMUNITY_COMMENT_ID BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- CommentTable 특화 컬럼들
    POST_ID              BIGINT        NOT NULL,
    AUTHOR_EMAIL         VARCHAR(255)  NOT NULL,
    CONTENT              TEXT          NOT NULL,
    PARENT_COMMENT_ID    BIGINT        NULL,

    -- BaseTable 공통 컬럼들
    CREATED_DATE         DATETIME      DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY           VARCHAR(255)  DEFAULT 'MEET_AGAIN',
    LAST_MODIFIED_DATE   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    LAST_MODIFIED_BY     VARCHAR(255)  DEFAULT 'MEET_AGAIN',
    DELETED              BOOLEAN       DEFAULT FALSE,

    -- 인덱스
    INDEX idx_community_comment_post_id (POST_ID),
    INDEX idx_community_comment_parent_id (PARENT_COMMENT_ID)
);
```

- `POST_ID`: 댓글이 속한 게시글 ID. FK 미사용, 애플리케이션에서 존재 확인
- `PARENT_COMMENT_ID`: 대댓글인 경우 부모 댓글 ID. NULL이면 일반 댓글
- `idx_community_comment_post_id`: 게시글별 댓글 목록 조회 시 사용
- `idx_community_comment_parent_id`: 부모 댓글별 대댓글 조회 시 사용

---

### 3.2 Domain Model -- NewComment

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/NewComment.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

class NewComment(
    val postId: Long,
    val authorEmail: String,
    val content: String,
    val parentCommentId: Long? = null,
) {
    init {
        validateContent()
    }

    private fun validateContent() {
        require(content.isNotBlank()) { "댓글 내용은 비어있을 수 없습니다." }
        require(content.length <= MAX_CONTENT_LENGTH) { "댓글 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다." }
    }

    fun isReply(): Boolean = parentCommentId != null

    companion object {
        private const val MAX_CONTENT_LENGTH = 500
    }
}
```

- `postId`: 댓글이 달리는 게시글 ID
- `authorEmail`: 작성자 이메일 (인증 정보에서 추출)
- `content`: 댓글 내용 (1~500자)
- `parentCommentId`: 대댓글인 경우 부모 댓글 ID, 일반 댓글이면 null
- `isReply()`: 대댓글 여부 판단 메서드 -- Service에서 분기 판단에 사용

---

### 3.3 Domain Model -- Comment

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/Comment.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

import java.time.LocalDateTime

class Comment(
    val id: Long = 0L,
    val postId: Long,
    val authorEmail: String,
    val content: String,
    val parentCommentId: Long? = null,
    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun isReply(): Boolean = parentCommentId != null

    fun validateNotReply() {
        require(!isReply()) { "대댓글에는 답글을 달 수 없습니다." }
    }
}
```

- `Post` 도메인 모델과 동일한 패턴
- `validateNotReply()`: 부모 댓글이 이미 대댓글인 경우 2단계 중첩을 방지하는 검증 메서드. Service에서 부모 댓글 조회 후 호출

---

### 3.4 Domain Exception -- ReplyDepthExceededException

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/exception/ReplyDepthExceededException.kt`

```kotlin
package com.konkuk.ma.domain.community.exception

import com.konkuk.ma.exception.BusinessException

class ReplyDepthExceededException(
    parentCommentId: Long,
) : BusinessException(
    message = "대댓글에는 답글을 달 수 없습니다.",
    dataMessage = "parentCommentId: $parentCommentId",
    logLevel = LogLevel.WARN,
)
```

- `Comment.validateNotReply()`에서 `require` 실패 시 사용하는 것 대신, Service에서 명시적으로 던지는 비즈니스 예외
- `LogLevel.WARN`: 잘못된 요청이므로 WARN 레벨

> **설계 변경**: `Comment.validateNotReply()`는 `require`로 `IllegalArgumentException`을 던지지 않고, Service에서 직접 `ReplyDepthExceededException`을 던지는 방식으로 변경한다. 이유: 비즈니스 예외는 `BusinessException`을 상속해야 `GlobalExceptionHandler`에서 일관되게 처리되기 때문이다.

따라서 `Comment.validateNotReply()` 대신 `Comment.isReply()`만 사용하고, Service에서 분기한다:

```kotlin
// CommentCommandService 내
if (parentComment.isReply()) {
    throw ReplyDepthExceededException(parentCommentId)
}
```

---

### 3.5 EntityType 수정

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/exception/EntityType.kt`

```kotlin
package com.konkuk.ma.exception

enum class EntityType(val entityName: String, val keyName: String) {
    MEMBER("Member", "email"),
    MATCHING_RESULT("MatchingResult", "id"),
    REFRESH_TOKEN("RefreshToken", "email"),
    COMMUNITY_POST("CommunityPost", "id"),
    COMMUNITY_COMMENT("CommunityComment", "id"),  // 추가
}
```

- `EntityNotFoundException`에서 사용할 댓글 엔티티 타입 추가

---

### 3.6 Domain Port -- CommentCommandRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/CommentCommandRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.NewComment

interface CommentCommandRepository {
    fun save(newComment: NewComment): Long
}
```

- `PostCommandRepository.save()` 패턴과 동일
- 반환값: 생성된 댓글 ID

---

### 3.7 Domain Port -- CommentQueryRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/CommentQueryRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.Comment

interface CommentQueryRepository {
    fun findOne(id: Long): Comment
}
```

- non-null 반환 원칙: 엔티티가 없으면 Repository 구현체에서 `EntityNotFoundException` 발생
- 대댓글 작성 시 부모 댓글 존재 확인 + 대댓글 여부 검증에 사용

---

### 3.8 Domain Port 수정 -- PostQueryRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/PostQueryRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory

interface PostQueryRepository {
    fun find(category: PostCategory?, cursorCondition: CursorIdCondition): CursorResult<List<Post>>
    fun findOne(id: Long): Post  // 추가
}
```

- `findOne(id: Long): Post`: non-null 반환. 게시글이 없으면 Repository에서 예외
- 댓글 작성 시 게시글 존재 확인에 사용

---

### 3.9 Domain Port 수정 -- PostCommandRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/PostCommandRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.NewPost

interface PostCommandRepository {
    fun save(newPost: NewPost): Long
    fun incrementComments(id: Long)  // 추가
}
```

- `incrementComments`: 댓글 작성 시 게시글의 `COMMENTS` 카운트를 1 증가
- 반환값 없음 (void) -- 업데이트 결과 확인 불필요 (게시글 존재는 이미 확인됨)

---

### 3.10 Domain Service -- CommentCommandService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/application/CommentCommandService.kt`

```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.exception.ReplyDepthExceededException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentCommandService(
    private val postQueryRepository: PostQueryRepository,
    private val postCommandRepository: PostCommandRepository,
    private val commentCommandRepository: CommentCommandRepository,
    private val commentQueryRepository: CommentQueryRepository,
) {
    fun create(newComment: NewComment): Long {
        postQueryRepository.findOne(newComment.postId)
        validateParentComment(newComment)
        val commentId = commentCommandRepository.save(newComment)
        postCommandRepository.incrementComments(newComment.postId)
        return commentId
    }

    private fun validateParentComment(newComment: NewComment) {
        if (!newComment.isReply()) return
        val parentComment = commentQueryRepository.findOne(newComment.parentCommentId!!)
        if (parentComment.isReply()) {
            throw ReplyDepthExceededException(newComment.parentCommentId)
        }
    }
}
```

- `PostCommandService.create()` 패턴을 따르되, 대댓글 검증 로직 추가
- Service는 조합만 담당 (규칙 11):
  1. 게시글 존재 확인 (`findOne` -- non-null이므로 없으면 예외 자동 발생)
  2. 대댓글인 경우 부모 댓글 존재 확인 + 2단계 중첩 방지 검증
  3. 댓글 저장
  4. 게시글 댓글 카운트 증가
- `validateParentComment`: 부모 댓글이 지정된 경우에만 검증 수행. 부모가 이미 대댓글이면 `ReplyDepthExceededException` 발생

---

### 3.11 Infrastructure -- CommentTable

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/entity/table/CommentTable.kt`

```kotlin
package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object CommentTable : BaseTable("COMMUNITY_COMMENTS", "COMMUNITY_COMMENT_ID") {
    val postId = long("POST_ID")
    val authorEmail = varchar("AUTHOR_EMAIL", 255)
    val content = text("CONTENT")
    val parentCommentId = long("PARENT_COMMENT_ID").nullable()
}
```

- `PostTable` 패턴과 동일하게 `BaseTable` 상속
- `parentCommentId`: nullable -- 일반 댓글이면 null

---

### 3.12 Infrastructure -- CommentEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/entity/CommentEntity.kt`

```kotlin
package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.Comment
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class CommentEntity(
    val id: Long,
    val postId: Long,
    val authorEmail: String,
    val content: String,
    val parentCommentId: Long?,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Comment {
        return Comment(
            id = id,
            postId = postId,
            authorEmail = authorEmail,
            content = content,
            parentCommentId = parentCommentId,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): CommentEntity {
            return CommentEntity(
                id = row[CommentTable.id].value,
                postId = row[CommentTable.postId],
                authorEmail = row[CommentTable.authorEmail],
                content = row[CommentTable.content],
                parentCommentId = row[CommentTable.parentCommentId],
                createdDate = row[CommentTable.createdDate],
            )
        }
    }
}
```

- `PostEntity` 패턴과 동일: `from(ResultRow)` 팩토리 + `toDomain()` 변환
- `parentCommentId`: nullable 그대로 유지

---

### 3.13 Infrastructure -- CommentCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/CommentCommandDao.kt`

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class CommentCommandDao {
    fun save(newComment: NewComment): Long {
        return CommentTable.insertAndGetId {
            it[postId] = newComment.postId
            it[authorEmail] = newComment.authorEmail
            it[content] = newComment.content
            it[parentCommentId] = newComment.parentCommentId
            it[createdBy] = newComment.authorEmail
            it[lastModifiedBy] = newComment.authorEmail
        }.value
    }
}
```

- `PostCommandDao.save()` 패턴과 동일
- `parentCommentId`가 null이면 DB에 NULL로 저장

---

### 3.14 Infrastructure -- CommentQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/CommentQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentEntity
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class CommentQueryDao {
    fun findOne(id: Long): CommentEntity? {
        return CommentTable
            .selectAll()
            .where { (CommentTable.id eq id) and (CommentTable.deleted eq false) }
            .map { row -> CommentEntity.from(row) }
            .singleOrNull()
    }
}
```

- nullable 반환: Repository에서 null 체크 후 `EntityNotFoundException` 발생
- `deleted eq false` 조건으로 논리 삭제된 댓글 제외

---

### 3.15 Infrastructure -- CommentCommandCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/repository/CommentCommandCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.CommentCommandDao
import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import org.springframework.stereotype.Repository

@Repository
class CommentCommandCoreRepository(
    private val commentCommandDao: CommentCommandDao,
) : CommentCommandRepository {
    override fun save(newComment: NewComment): Long {
        return commentCommandDao.save(newComment)
    }
}
```

- `PostCommandCoreRepository` 패턴과 동일: DAO에 위임

---

### 3.16 Infrastructure -- CommentQueryCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/repository/CommentQueryCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.CommentQueryDao
import com.konkuk.ma.domain.community.domain.Comment
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class CommentQueryCoreRepository(
    private val commentQueryDao: CommentQueryDao,
) : CommentQueryRepository {
    override fun findOne(id: Long): Comment {
        return commentQueryDao.findOne(id)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.COMMUNITY_COMMENT, id.toString())
    }
}
```

- non-null 반환 원칙: DAO 결과가 null이면 `EntityNotFoundException` 발생
- `MatchingResultCoreRepository` 패턴과 동일

---

### 3.17 Infrastructure 수정 -- PostQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/PostQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostEntity
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PostQueryDao {
    // 기존 메서드 유지
    fun find(category: String?, cursorId: Long?, size: Int): List<PostEntity> {
        return PostTable
            .selectAll()
            .where {
                var condition: Op<Boolean> = PostTable.deleted eq false
                if (category != null) {
                    condition = condition and (PostTable.category eq category)
                }
                if (cursorId != null) {
                    condition = condition and (PostTable.id less cursorId)
                }
                condition
            }
            .orderBy(PostTable.id to SortOrder.DESC)
            .limit(size)
            .map { row -> PostEntity.from(row) }
    }

    fun findOne(id: Long): PostEntity? {  // 추가
        return PostTable
            .selectAll()
            .where { (PostTable.id eq id) and (PostTable.deleted eq false) }
            .map { row -> PostEntity.from(row) }
            .singleOrNull()
    }
}
```

- `findOne`: 단건 게시글 조회. `deleted eq false` 조건 포함
- `singleOrNull()`: 결과가 0건이면 null, 1건이면 반환

---

### 3.18 Infrastructure 수정 -- PostQueryCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/repository/PostQueryCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.dao.PostQueryDao
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class PostQueryCoreRepository(
    private val postQueryDao: PostQueryDao,
) : PostQueryRepository {
    // 기존 메서드 유지
    override fun find(category: PostCategory?, cursorCondition: CursorIdCondition): CursorResult<List<Post>> {
        val posts = postQueryDao.find(category?.name, cursorCondition.cursorId, cursorCondition.size)
            .map { it.toDomain() }

        return CursorResult.of(posts, cursorCondition.size) { it.id }
    }

    override fun findOne(id: Long): Post {  // 추가
        return postQueryDao.findOne(id)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.COMMUNITY_POST, id.toString())
    }
}
```

- non-null 반환: 게시글이 없으면 `EntityNotFoundException` 발생

---

### 3.19 Infrastructure 수정 -- PostCommandDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/PostCommandDao.kt`

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class PostCommandDao {
    // 기존 메서드 유지
    fun save(newPost: NewPost): Long {
        return PostTable.insertAndGetId {
            it[authorEmail] = newPost.authorEmail
            it[category] = newPost.category.name
            it[title] = newPost.title
            it[content] = newPost.content
            it[createdBy] = newPost.authorEmail
            it[lastModifiedBy] = newPost.authorEmail
        }.value
    }

    fun incrementComments(id: Long) {  // 추가
        PostTable.update({ PostTable.id eq id }) {
            it[comments] = comments + 1
        }
    }
}
```

- `SqlExpressionBuilder.plus`: Exposed에서 컬럼 값을 1 증가시키는 DSL
- `comments + 1`: SQL의 `COMMENTS = COMMENTS + 1`로 변환됨 (원자적 증가)

---

### 3.20 Infrastructure 수정 -- PostCommandCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/repository/PostCommandCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.PostCommandDao
import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import org.springframework.stereotype.Repository

@Repository
class PostCommandCoreRepository(
    private val postCommandDao: PostCommandDao,
) : PostCommandRepository {
    // 기존 메서드 유지
    override fun save(newPost: NewPost): Long {
        return postCommandDao.save(newPost)
    }

    override fun incrementComments(id: Long) {  // 추가
        postCommandDao.incrementComments(id)
    }
}
```

---

### 3.21 ValidationMessages 수정

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/support/validation/ValidationPatterns.kt`

```kotlin
object ValidationMessages {
    // ... 기존 상수 유지 ...
    const val POST_CONTENT_REQUIRED = "내용은 필수입니다."
    const val COMMENT_CONTENT_REQUIRED = "댓글 내용은 필수입니다."       // 추가
    const val COMMENT_CONTENT_SIZE = "댓글 내용은 500자 이하여야 합니다."  // 추가
}
```

---

### 3.22 API Request DTO -- NewCommentRequest

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/request/NewCommentRequest.kt`

```kotlin
package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class NewCommentRequest(
    @field:NotBlank(message = ValidationMessages.COMMENT_CONTENT_REQUIRED)
    @field:Size(max = MAX_CONTENT_LENGTH, message = ValidationMessages.COMMENT_CONTENT_SIZE)
    val content: String,

    val parentCommentId: Long? = null,
) {
    fun toNewComment(authorEmail: String, postId: Long): NewComment {
        return NewComment(
            postId = postId,
            authorEmail = authorEmail,
            content = content,
            parentCommentId = parentCommentId,
        )
    }

    companion object {
        private const val MAX_CONTENT_LENGTH = 500
    }
}
```

- `NewPostRequest` 패턴과 동일: `toNewComment()` 팩토리 메서드로 도메인 객체 생성
- `parentCommentId`: nullable -- JSON에서 생략하거나 null이면 일반 댓글
- `content`에 `@NotBlank` + `@Size(max=500)` 적용

---

### 3.23 API Response DTO -- NewCommentResponse

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/response/NewCommentResponse.kt`

```kotlin
package com.konkuk.ma.domain.community.api.response

class NewCommentResponse(
    val commentId: Long,
)
```

- `NewPostResponse` 패턴과 동일

---

### 3.24 Controller -- CommunityCommentCommandApi

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/CommunityCommentCommandApi.kt`

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.request.NewCommentRequest
import com.konkuk.ma.domain.community.api.response.NewCommentResponse
import com.konkuk.ma.domain.community.application.CommentCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts/{postId}/comments")
class CommunityCommentCommandApi(
    private val commentCommandService: CommentCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal email: String,
        @PathVariable postId: Long,
        @Valid @RequestBody request: NewCommentRequest,
    ): NewCommentResponse {
        val commentId = commentCommandService.create(request.toNewComment(email, postId))
        return NewCommentResponse(commentId = commentId)
    }
}
```

- `CommunityPostCommandApi` 패턴과 동일
- `@PathVariable postId`: URL 경로에서 게시글 ID 추출
- `request.toNewComment(email, postId)`: Request DTO에서 도메인 객체로 변환

---

### 3.25 테스트

**NewCommentTest**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/community/domain/NewCommentTest.kt`
- content가 빈 문자열이면 `IllegalArgumentException` 발생
- content가 500자 초과이면 `IllegalArgumentException` 발생
- 정상 생성 확인 (일반 댓글, 대댓글)
- `isReply()` 동작 검증

**CommentCommandServiceTest**: `domain/ma-domain-core/src/test/kotlin/com/konkuk/ma/domain/community/application/CommentCommandServiceTest.kt`
- 일반 댓글 작성 성공
- 대댓글 작성 성공 (부모가 일반 댓글인 경우)
- 대댓글의 대댓글 시도 시 `ReplyDepthExceededException` 발생
- 존재하지 않는 게시글에 댓글 시도 시 `EntityNotFoundException` 발생
- 존재하지 않는 부모 댓글에 답글 시도 시 `EntityNotFoundException` 발생

**CommunityCommentCommandApiTest**: `boot/ma-boot-web/src/test/kotlin/com/konkuk/ma/domain/community/api/CommunityCommentCommandApiTest.kt`
- REST Docs 문서화 포함
- 일반 댓글 작성 성공 (201 Created)
- 대댓글 작성 성공 (201 Created)
- content가 빈 경우 400 Bad Request

---

### 3.26 REST Docs

**CommunityVocabulary.kt**: 댓글 관련 request/response 필드 vocabulary 추가
**AsciiDoc**: `community/create-comment.adoc` 생성
**main.adoc**: `create-comment` 연결 추가

---

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `infrastructure/.../script/ddl.sql` | 수정 | COMMUNITY_COMMENTS 테이블 DDL 추가 |
| 2 | `domain/.../community/domain/NewComment.kt` | 신규 | 댓글 생성 도메인 모델 |
| 3 | `domain/.../community/domain/Comment.kt` | 신규 | 댓글 도메인 모델 |
| 4 | `domain/.../community/exception/ReplyDepthExceededException.kt` | 신규 | 2단계 중첩 방지 비즈니스 예외 |
| 5 | `domain/.../exception/EntityType.kt` | 수정 | COMMUNITY_COMMENT 추가 |
| 6 | `domain/.../community/domain/port/CommentCommandRepository.kt` | 신규 | 댓글 저장 포트 |
| 7 | `domain/.../community/domain/port/CommentQueryRepository.kt` | 신규 | 댓글 조회 포트 |
| 8 | `domain/.../community/domain/port/PostQueryRepository.kt` | 수정 | findOne(id) 추가 |
| 9 | `domain/.../community/domain/port/PostCommandRepository.kt` | 수정 | incrementComments(id) 추가 |
| 10 | `domain/.../community/application/CommentCommandService.kt` | 신규 | 댓글 작성 서비스 (조합 담당) |
| 11 | `infrastructure/.../community/entity/table/CommentTable.kt` | 신규 | Exposed 테이블 정의 |
| 12 | `infrastructure/.../community/entity/CommentEntity.kt` | 신규 | Entity + from/toDomain |
| 13 | `infrastructure/.../community/dao/CommentCommandDao.kt` | 신규 | insertAndGetId 구현 |
| 14 | `infrastructure/.../community/dao/CommentQueryDao.kt` | 신규 | findOne 구현 |
| 15 | `infrastructure/.../community/repository/CommentCommandCoreRepository.kt` | 신규 | 포트 구현체 (Command) |
| 16 | `infrastructure/.../community/repository/CommentQueryCoreRepository.kt` | 신규 | 포트 구현체 (Query) |
| 17 | `infrastructure/.../community/dao/PostQueryDao.kt` | 수정 | findOne(id) 추가 |
| 18 | `infrastructure/.../community/repository/PostQueryCoreRepository.kt` | 수정 | findOne 구현 |
| 19 | `infrastructure/.../community/dao/PostCommandDao.kt` | 수정 | incrementComments 추가 |
| 20 | `infrastructure/.../community/repository/PostCommandCoreRepository.kt` | 수정 | incrementComments 위임 |
| 21 | `boot/.../support/validation/ValidationPatterns.kt` | 수정 | COMMENT_CONTENT_REQUIRED/SIZE 추가 |
| 22 | `boot/.../community/api/request/NewCommentRequest.kt` | 신규 | Request DTO + toNewComment() |
| 23 | `boot/.../community/api/response/NewCommentResponse.kt` | 신규 | Response DTO |
| 24 | `boot/.../community/api/CommunityCommentCommandApi.kt` | 신규 | Controller |
| 25 | `domain/.../community/domain/NewCommentTest.kt` | 신규 | 도메인 모델 유효성 검증 테스트 |
| 26 | `domain/.../community/application/CommentCommandServiceTest.kt` | 신규 | 서비스 조합 테스트 |
| 27 | `boot/.../community/api/CommunityCommentCommandApiTest.kt` | 신규 | API + REST Docs 테스트 |

---

## 5. 고려사항

- **2단계 중첩 방지**: `PARENT_COMMENT_ID`가 있는 댓글(대댓글)에 다시 답글을 달려고 하면, 부모 댓글을 조회하여 `isReply()` 검증 후 `ReplyDepthExceededException`을 발생시킨다. DB 레벨 제약은 두지 않고 애플리케이션에서 검증한다.

- **댓글 카운트 정합성**: `PostTable.COMMENTS` 컬럼을 `comments + 1`로 원자적으로 증가시킨다. 동시성 이슈는 DB 레벨의 row lock으로 해결된다 (MariaDB InnoDB의 UPDATE는 exclusive lock 획득).

- **FK 미사용**: DDL에 FOREIGN KEY를 사용하지 않는다 (프로젝트 규칙). `POST_ID`, `PARENT_COMMENT_ID`의 참조 무결성은 애플리케이션 레벨에서 `findOne` + `EntityNotFoundException`으로 보장한다.

- **authorNickname 비정규화 안 함**: 기존 결정에 따라 `COMMUNITY_COMMENTS` 테이블에 닉네임 컬럼을 넣지 않는다. 댓글 목록 조회 API 구현 시 `MemberQueryRepository`로 닉네임을 조합한다.

- **인덱스 설계**: `idx_community_comment_post_id`는 게시글별 댓글 목록 조회에 필수. `idx_community_comment_parent_id`는 부모 댓글별 대댓글 조회 및 대댓글 작성 시 부모 검증에 사용된다.

- **삭제 시 댓글 카운트 감소**: 댓글 삭제 API 구현 시 `decrementComments` 메서드를 추가해야 한다. 현재 스코프에서는 작성 API만 다루므로 포함하지 않는다.

- **Exposed `long()` vs `reference()`**: `parentCommentId`에 `reference()`를 쓰면 Exposed가 FK를 자동 생성하므로, FK 미사용 원칙에 따라 `long().nullable()`로 선언한다.

---

## 핵심 규칙 적용

- **규칙 5 (포트는 List 반환)**: 단건 조회 포트는 non-null 반환, Repository에서 예외 처리
- **규칙 8 (DAO -> Entity -> Domain)**: CommentQueryDao -> CommentEntity -> Comment 변환 체인
- **규칙 10 (ValidationMessages 상수)**: COMMENT_CONTENT_REQUIRED, COMMENT_CONTENT_SIZE 상수 사용
- **규칙 11 (Service는 조합만)**: CommentCommandService는 포트 호출과 검증만 수행
- **규칙 14 (메서드 네이밍)**: findOne(단건), find(복수) 구분
- **규칙 15 (RESTful URL)**: POST /api/community/posts/{postId}/comments

## 검증

1. `./gradlew compileKotlin` -- 컴파일 확인
2. `./gradlew test` -- 전체 테스트 통과
3. REST Docs snippet 생성 확인
