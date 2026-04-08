# Design: 커뮤니티 게시글 상세 조회 API

> 작성일: 2026-04-06
> 상태: Draft

## 1. 설계 개요

인증된 사용자가 `GET /api/community/posts/{id}`로 게시글 상세 정보를 조회하는 API를 구현한다. 게시글 정보(제목, 내용, 카테고리, 좋아요 수, 작성자 닉네임, timeAgo)와 함께 댓글 목록을 반환하되, 각 댓글의 대댓글은 최신순 3개까지만 내용을 포함하고 나머지는 개수만 표시한다.

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                         │
│                                                          │
│  PostQueryApi                                            │
│    GET /api/community/posts/{id}                         │
│    @AuthenticationPrincipal email                        │
│      └── PostQueryService.findDetail(id)                 │
│      └── PostDetailResponse.from(postDetail)             │
└────────────────────────┬─────────────────────────────────┘
                         │ (port)
┌────────────────────────▼─────────────────────────────────┐
│ domain/ma-domain-core                                    │
│                                                          │
│  community/domain/                                       │
│    PostDetail(post, nickname, comments)                   │
│    CommentWithAuthor(comment, nickname, replies,          │
│                      remainingReplyCount)                 │
│    Comments — 일급 컬렉션 (댓글 그룹핑/조합 행위)         │
│  community/domain/port/                                  │
│    PostQueryRepository                                   │
│      + findOne(id: Long): Post   (기존)                  │
│    CommentQueryRepository                                │
│      + find(postId: Long): List<Comment>   (추가)        │
│  community/application/                                  │
│    PostQueryService                                      │
│      + findDetail(id: Long): PostDetail   (추가)         │
└────────────────────────┬─────────────────────────────────┘
                         │ (implements)
┌────────────────────────▼─────────────────────────────────┐
│ infrastructure/storage/ma-db-core                        │
│                                                          │
│  community/dao/CommentQueryDao                           │
│    + find(postId: Long): List<CommentEntity>   (추가)    │
│  community/repository/CommentQueryCoreRepository         │
│    + find(postId: Long): List<Comment>   (추가)          │
└──────────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 Domain — CommentWithAuthor

게시글 상세에서 댓글을 표현하는 도메인 객체. 댓글 본문 + 작성자 닉네임 + 대댓글 목록 + 나머지 대댓글 개수를 has-a 관계로 조합한다.

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/CommentWithAuthor.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

class CommentWithAuthor(
    val comment: Comment,
    val nickname: String,
    val replies: List<ReplyWithAuthor> = emptyList(),
    val remainingReplyCount: Int = 0,
)
```

- `comment`: has-a 관계로 Comment 도메인 객체 참조 (Post와 PostWithAuthor 패턴 동일)
- `nickname`: Member 조합으로 가져온 작성자 닉네임
- `replies`: 최신순 3개까지의 대댓글 목록
- `remainingReplyCount`: 3개 초과 시 나머지 대댓글 수 (전체 - 3)

### 3.2 Domain — ReplyWithAuthor

대댓글 표현 도메인 객체. CommentWithAuthor와 구조적으로 동일하지만, 대댓글에는 하위 댓글이 없으므로 별도 클래스로 분리한다.

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/ReplyWithAuthor.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

class ReplyWithAuthor(
    val comment: Comment,
    val nickname: String,
)
```

- 대댓글은 더 이상 하위 대댓글을 가지지 않으므로 `replies` 필드 없음
- Comment를 has-a로 참조

### 3.3 Domain — Comments (일급 컬렉션)

댓글 목록에서 부모/대댓글 그룹핑, 작성자 닉네임 조합, 대댓글 3개 제한 등 비즈니스 로직을 응집시키는 일급 컬렉션.

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/Comments.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<String> {
        return data.map { it.authorEmail }.toSet()
    }

    fun combineWithAuthors(members: Members): List<CommentWithAuthor> {
        val parentComments = data.filter { !it.hasParent() }
        val repliesByParentId = data.filter { it.hasParent() }
            .groupBy { it.parentCommentId!! }

        return parentComments.map { parent ->
            val allReplies = repliesByParentId[parent.id].orEmpty()
                .sortedByDescending { it.createdDate }
            val previewReplies = allReplies.take(REPLY_PREVIEW_COUNT)
            val remainingCount = (allReplies.size - REPLY_PREVIEW_COUNT).coerceAtLeast(0)

            CommentWithAuthor(
                comment = parent,
                nickname = members.findNicknameByEmail(parent.authorEmail),
                replies = previewReplies.map { reply ->
                    ReplyWithAuthor(
                        comment = reply,
                        nickname = members.findNicknameByEmail(reply.authorEmail),
                    )
                },
                remainingReplyCount = remainingCount,
            )
        }
    }

    companion object {
        private const val REPLY_PREVIEW_COUNT = 3
    }
}
```

- `extractAuthorEmails()`: 댓글 + 대댓글 전체의 작성자 이메일 추출 (벌크 조회용)
- `combineWithAuthors()`: 부모 댓글 기준으로 그룹핑 → 대댓글 최신순 정렬 → 3개 제한 → CommentWithAuthor 조합
- `REPLY_PREVIEW_COUNT = 3`: 대댓글 미리보기 개수 상수

### 3.4 Domain — PostDetail

게시글 상세 조회 결과를 표현하는 도메인 객체. Post + 작성자 닉네임 + 댓글 목록을 조합한다.

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/PostDetail.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

class PostDetail(
    val post: Post,
    val nickname: String,
    val comments: List<CommentWithAuthor>,
)
```

- `post`: has-a (PostWithAuthor 패턴과 동일)
- `nickname`: 게시글 작성자 닉네임
- `comments`: 댓글 목록 (대댓글 포함)

### 3.5 Domain Port — CommentQueryRepository 수정

댓글 목록 조회 메서드를 추가한다.

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/CommentQueryRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.Comment

interface CommentQueryRepository {
    fun findOne(id: Long): Comment
    fun find(postId: Long): List<Comment>  // 추가
}
```

- `find(postId)`: 특정 게시글의 댓글 + 대댓글 전체를 조회
- 반환 타입은 `List<Comment>` (포트는 일급 컬렉션 반환하지 않음, 규칙 5)
- Service에서 `Comments`로 감싸서 비즈니스 로직 처리

### 3.6 Domain Service — PostQueryService 수정

`findDetail` 메서드를 추가한다. Service는 조합만 담당하며 비즈니스 로직은 `Comments` 일급 컬렉션에 위임한다.

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/application/PostQueryService.kt`

```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Comments
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostDetail
import com.konkuk.ma.domain.community.domain.PostWithAuthor
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostQueryService(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,  // 추가
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun find(category: PostCategory?, cursorCondition: CursorIdCondition): CursorResult<List<PostWithAuthor>> {
        val cursorResult = postQueryRepository.find(category, cursorCondition)
        val posts = Posts(cursorResult.data)
        val members = Members(memberQueryRepository.findByEmails(posts.extractAuthorEmails()))

        return CursorResult(
            data = posts.combineWithAuthors(members),
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }

    fun findDetail(id: Long): PostDetail {  // 추가
        val post = postQueryRepository.findOne(id)
        val comments = Comments(commentQueryRepository.find(id))

        val authorEmails = comments.extractAuthorEmails() + post.authorEmail
        val members = Members(memberQueryRepository.findByEmails(authorEmails))

        return PostDetail(
            post = post,
            nickname = members.findNicknameByEmail(post.authorEmail),
            comments = comments.combineWithAuthors(members),
        )
    }
}
```

- `findDetail`: 게시글 + 댓글 + 작성자 닉네임을 조합
- 게시글 작성자 이메일을 댓글 작성자 이메일과 합쳐서 **한 번의 벌크 쿼리**로 Member 조회 (N+1 방지)
- 비즈니스 로직(그룹핑, 대댓글 제한)은 `Comments.combineWithAuthors()`에 위임

### 3.7 Infrastructure DAO — CommentQueryDao 수정

게시글 ID로 댓글 전체를 조회하는 메서드를 추가한다.

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/CommentQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.CommentEntity
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.SortOrder
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

    fun find(postId: Long): List<CommentEntity> {  // 추가
        return CommentTable
            .selectAll()
            .where { (CommentTable.postId eq postId) and (CommentTable.deleted eq false) }
            .orderBy(CommentTable.id to SortOrder.ASC)
            .map { row -> CommentEntity.from(row) }
    }
}
```

- `find(postId)`: 특정 게시글의 전체 댓글(부모+대댓글) 조회
- `orderBy(id ASC)`: 부모 댓글은 작성순으로 정렬. 대댓글 최신순 정렬은 `Comments` 일급 컬렉션에서 처리
- `deleted eq false`: 삭제된 댓글 제외
- SortOrder import 추가 필요

### 3.8 Infrastructure Repository — CommentQueryCoreRepository 수정

포트 구현체에 `find` 메서드를 추가한다.

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

    override fun find(postId: Long): List<Comment> {  // 추가
        return commentQueryDao.find(postId).map { it.toDomain() }
    }
}
```

- DAO에서 `List<CommentEntity>` 반환 → `.map { it.toDomain() }`으로 도메인 변환
- 빈 리스트 반환 가능 (댓글이 없는 게시글)

### 3.9 Boot API — PostQueryApi 수정

게시글 상세 조회 엔드포인트를 추가한다.

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/PostQueryApi.kt`

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.community.api.response.PostDetailResponse
import com.konkuk.ma.domain.community.api.response.PostResponse
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.support.payload.response.CursorResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class PostQueryApi(
    private val postQueryService: PostQueryService,
) {
    @GetMapping
    fun findPosts(
        @RequestParam(required = false) category: PostCategory?,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(required = false) size: Int?,
    ): CursorResponse<List<PostResponse>> {
        val cursorResult = postQueryService.find(category, CursorIdCondition.of(cursorId, size))
        return CursorResponse(
            data = cursorResult.data.map { PostResponse.from(it) },
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }

    @GetMapping("/{id}")  // 추가
    fun findDetail(
        @PathVariable id: Long,
    ): PostDetailResponse {
        val postDetail = postQueryService.findDetail(id)
        return PostDetailResponse.from(postDetail)
    }
}
```

- `@GetMapping("/{id}")`: RESTful 패턴 준수
- `@PathVariable id: Long`: 기존 PostLikeApi 패턴과 동일 (아직 `@DecryptId` 미사용)
- Api는 Service만 의존, 응답 변환은 `PostDetailResponse.from()`에 위임

### 3.10 Boot Response — PostDetailResponse

게시글 상세 조회 API 응답 DTO.

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/response/PostDetailResponse.kt`

```kotlin
package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.PostDetail

class PostDetailResponse(
    val id: Long,
    val nickname: String,
    val category: String,
    val title: String,
    val content: String,
    val likes: Int,
    val timeAgo: String,
    val comments: List<CommentResponse>,
) {
    companion object {
        fun from(postDetail: PostDetail): PostDetailResponse {
            val post = postDetail.post
            return PostDetailResponse(
                id = post.id,
                nickname = postDetail.nickname,
                category = post.category.name,
                title = post.title,
                content = post.content,
                likes = post.likes,
                timeAgo = TimeAgoCalculator.calculate(post.createdDate),
                comments = postDetail.comments.map { CommentResponse.from(it) },
            )
        }
    }
}
```

- 기존 `PostResponse`와 동일한 필드 + `comments` 추가
- `CommentResponse.from()`: 댓글 변환은 별도 Response DTO에 위임

### 3.11 Boot Response — CommentResponse

댓글 응답 DTO. 대댓글 목록과 나머지 대댓글 개수를 포함한다.

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/response/CommentResponse.kt`

```kotlin
package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.CommentWithAuthor

class CommentResponse(
    val id: Long,
    val nickname: String,
    val content: String,
    val likes: Int,
    val timeAgo: String,
    val replies: List<ReplyResponse>,
    val remainingReplyCount: Int,
) {
    companion object {
        fun from(commentWithAuthor: CommentWithAuthor): CommentResponse {
            val comment = commentWithAuthor.comment
            return CommentResponse(
                id = comment.id,
                nickname = commentWithAuthor.nickname,
                content = comment.content,
                likes = comment.likes,
                timeAgo = TimeAgoCalculator.calculate(comment.createdDate),
                replies = commentWithAuthor.replies.map { ReplyResponse.from(it) },
                remainingReplyCount = commentWithAuthor.remainingReplyCount,
            )
        }
    }
}
```

### 3.12 Boot Response — ReplyResponse

대댓글 응답 DTO.

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/response/ReplyResponse.kt`

```kotlin
package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.ReplyWithAuthor

class ReplyResponse(
    val id: Long,
    val nickname: String,
    val content: String,
    val likes: Int,
    val timeAgo: String,
) {
    companion object {
        fun from(replyWithAuthor: ReplyWithAuthor): ReplyResponse {
            val comment = replyWithAuthor.comment
            return ReplyResponse(
                id = comment.id,
                nickname = replyWithAuthor.nickname,
                content = comment.content,
                likes = comment.likes,
                timeAgo = TimeAgoCalculator.calculate(comment.createdDate),
            )
        }
    }
}
```

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/.../community/domain/ReplyWithAuthor.kt` | 신규 | 대댓글+닉네임 도메인 객체 |
| 2 | `domain/.../community/domain/CommentWithAuthor.kt` | 신규 | 댓글+닉네임+대댓글 도메인 객체 |
| 3 | `domain/.../community/domain/Comments.kt` | 신규 | 댓글 일급 컬렉션 (그룹핑, 조합 행위) |
| 4 | `domain/.../community/domain/PostDetail.kt` | 신규 | 게시글 상세 도메인 객체 |
| 5 | `domain/.../community/domain/port/CommentQueryRepository.kt` | 수정 | `find(postId)` 메서드 추가 |
| 6 | `infrastructure/.../community/dao/CommentQueryDao.kt` | 수정 | `find(postId)` 메서드 추가 |
| 7 | `infrastructure/.../community/repository/CommentQueryCoreRepository.kt` | 수정 | `find(postId)` 구현 추가 |
| 8 | `domain/.../community/application/PostQueryService.kt` | 수정 | `findDetail(id)` 메서드 추가, `CommentQueryRepository` 의존성 추가 |
| 9 | `boot/.../community/api/response/ReplyResponse.kt` | 신규 | 대댓글 응답 DTO |
| 10 | `boot/.../community/api/response/CommentResponse.kt` | 신규 | 댓글 응답 DTO |
| 11 | `boot/.../community/api/response/PostDetailResponse.kt` | 신규 | 게시글 상세 응답 DTO |
| 12 | `boot/.../community/api/PostQueryApi.kt` | 수정 | `findDetail` 엔드포인트 추가 |

## 5. 고려사항

- **N+1 방지**: 게시글 작성자 이메일과 댓글 작성자 이메일을 합쳐서 `memberQueryRepository.findByEmails()`를 **한 번만** 호출한다. 댓글마다 Member를 조회하지 않는다.

- **대댓글 제한 전략**: 전체 댓글을 한 번에 조회한 뒤 메모리에서 그룹핑/제한한다. DB에서 대댓글 3개 제한 쿼리를 작성하면 복잡도가 높아지고(ROW_NUMBER 윈도우 함수 필요), 현 단계에서는 댓글 수가 대량이 아닐 것으로 예상되므로 메모리 처리가 적합하다. 향후 댓글이 대량화되면 별도 대댓글 조회 API 분리를 고려한다.

- **좋아요 수**: 현재 `PostTable.likes` 컬럼(비정규화)을 사용한다. 요구사항에 명시된 대로 아직 POST_LIKES 테이블에서 count하지 않고, 기존 likes 컬럼을 그대로 활용한다.

- **댓글 정렬**: 부모 댓글은 `id ASC`(작성순), 대댓글은 `createdDate DESC`(최신순)으로 정렬한다. 부모 댓글 정렬은 DAO에서, 대댓글 정렬은 `Comments` 일급 컬렉션에서 처리한다.

- **인덱스**: `CommentTable`의 `POST_ID` 컬럼에 인덱스가 필요하다. 게시글별 댓글 조회 시 `WHERE POST_ID = ? AND DELETED = false` 쿼리가 빈번하게 실행되므로, `(POST_ID, DELETED)` 복합 인덱스 또는 `POST_ID` 단일 인덱스 추가를 권장한다. 단, DDL 변경은 이 API 구현 범위에서는 포함하지 않고 별도로 진행한다.

- **ID 난독화**: 기존 PostLikeApi가 `@PathVariable postId: Long`을 그대로 사용하므로 동일 패턴 유지. 향후 `@DecryptId` 적용 시 일괄 변경.

- **PostQueryService 의존성 추가**: `CommentQueryRepository` 의존성이 추가된다. Service가 Repository(포트)만 의존하므로 규칙 11(Service는 Service를 참조하지 않는다)을 준수한다. 별도 CommentQueryService를 만들지 않는 이유는, 게시글 상세 조회가 게시글 + 댓글을 **한 트랜잭션**에서 조합해야 하기 때문이다.
