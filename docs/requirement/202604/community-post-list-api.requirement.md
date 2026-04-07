# Design: 커뮤니티 게시글 목록 조회 API

> 작성일: 2026-04-06
> 상태: Draft

## 1. 설계 개요

커뮤니티 도메인을 새로 생성하고, 카테고리별 게시글 목록을 Offset 기반 페이징으로 조회하는 `GET /api/community/posts` API를 구현한다. 응답에는 게시글 ID, 작성자 닉네임, 카테고리, 제목, 본문, 좋아요 수, 댓글 수, 경과 시간(timeAgo)을 포함한다.

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                     │
│                                                      │
│  CommunityPostQueryApi                               │
│    GET /api/community/posts                          │
│    @AuthenticationPrincipal email                    │
│    @RequestParam category, page                      │
│      └── PostQueryService.find(category, page)       │
│      └── PostsResponse.from(posts)                   │
└────────────────────────┬─────────────────────────────┘
                         │ (port)
┌────────────────────────▼─────────────────────────────┐
│ domain/ma-domain-core                                │
│                                                      │
│  community/domain/                                   │
│    Post, Posts, PostCategory                          │
│  community/domain/port/                              │
│    PostQueryRepository                               │
│      + find(category: PostCategory, page: Int): Posts │
│  community/application/                              │
│    PostQueryService                                  │
│      + find(category: PostCategory, page: Int): Posts │
└────────────────────────┬─────────────────────────────┘
                         │ (implements)
┌────────────────────────▼─────────────────────────────┐
│ infrastructure/storage/ma-db-core                    │
│                                                      │
│  community/entity/table/PostTable                    │
│  community/entity/PostEntity                         │
│  community/dao/PostQueryDao                          │
│    + find(category, pageSize, offset): List<PostEntity>│
│    + count(category): Long                           │
│  community/repository/PostQueryCoreRepository        │
│    → PostQueryDao 위임, Entity → Domain 변환         │
└──────────────────────────────────────────────────────┘
```

## 3. 상세 설계

### 3.1 DDL - COMMUNITY_POSTS 테이블

**파일**: `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql`

기존 ddl.sql 끝에 추가:

```sql
-- COMMUNITY POSTS
CREATE TABLE COMMUNITY_POSTS
(
    COMMUNITY_POST_ID  BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- PostTable 특화 컬럼들
    AUTHOR_EMAIL       VARCHAR(255)  NOT NULL,
    AUTHOR_NICKNAME    VARCHAR(255)  NOT NULL,
    CATEGORY           VARCHAR(32)   NOT NULL,
    TITLE              VARCHAR(100)  NOT NULL,
    CONTENT            TEXT          NOT NULL,
    LIKES              INT           DEFAULT 0,
    COMMENTS           INT           DEFAULT 0,

    -- BaseTable 공통 컬럼들
    CREATED_DATE       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY         VARCHAR(255)  DEFAULT 'MEET_AGAIN',
    LAST_MODIFIED_DATE DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    LAST_MODIFIED_BY   VARCHAR(255)  DEFAULT 'MEET_AGAIN',
    DELETED            BOOLEAN       DEFAULT FALSE,

    -- 인덱스
    INDEX idx_community_post_category (CATEGORY),
    INDEX idx_community_post_author_email (AUTHOR_EMAIL)
);
```

- `AUTHOR_NICKNAME`: 조회 시 JOIN 없이 닉네임을 바로 가져오기 위해 비정규화. 닉네임 변경 기능이 없는 현 시점에서 적합한 설계
- `LIKES`, `COMMENTS`: 카운트 쿼리 비용을 줄이기 위해 비정규화. 좋아요/댓글 생성 시 증감 처리
- `CATEGORY` 인덱스: 카테고리별 필터링 조회가 주 용도
- `TITLE`은 api-todo에서 max 40자 정의이나, 향후 확장을 고려해 DDL은 100자로 설정. 유효성 검증은 도메인에서 처리

### 3.2 Domain - PostCategory (Enum)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/PostCategory.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

enum class PostCategory {
    SUCCESS_STORY,
    CHEER,
    COUNSELING,
}
```

- api-todo 명세의 3가지 카테고리를 enum으로 정의

### 3.3 Domain - Post (도메인 모델)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/Post.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

import java.time.Duration
import java.time.LocalDateTime

class Post(
    val id: Long = 0L,
    val authorEmail: String,
    val authorNickname: String,
    val category: PostCategory,
    val title: String,
    val content: String,
    val likes: Int = 0,
    val comments: Int = 0,
    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun calculateTimeAgo(now: LocalDateTime = LocalDateTime.now()): String {
        val duration = Duration.between(createdDate, now)
        return when {
            duration.toMinutes() < 1 -> "방금 전"
            duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
            duration.toDays() < 1 -> "${duration.toHours()}시간 전"
            duration.toDays() < 30 -> "${duration.toDays()}일 전"
            duration.toDays() < 365 -> "${duration.toDays() / 30}개월 전"
            else -> "${duration.toDays() / 365}년 전"
        }
    }
}
```

- `calculateTimeAgo`: 도메인 객체에 행위를 부여하여 경과 시간을 계산. `now` 파라미터는 테스트 용이성을 위해 외부 주입 가능
- title/content 유효성 검증은 게시글 작성(POST) API에서 다루므로 이 설계에서는 생략

### 3.4 Domain - Posts (일급 컬렉션)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/Posts.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

class Posts(
    val data: List<Post>,
    val totalCount: Long,
    val currentPage: Int,
) {
    fun hasNext(): Boolean {
        return (currentPage + 1) * PAGE_SIZE < totalCount
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
```

- `totalCount`: 전체 게시글 수. 페이징 메타데이터 계산에 사용
- `currentPage`: 0-based 페이지 번호
- `PAGE_SIZE`: 한 페이지당 게시글 수. 상수로 관리 (현재 변동 가능성 낮음)
- `hasNext()`: 다음 페이지 존재 여부. 프론트엔드에서 "더보기" 버튼 표시에 활용

### 3.5 Domain Port - PostQueryRepository

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/PostQueryRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts

interface PostQueryRepository {
    fun find(category: PostCategory, page: Int): Posts
}
```

- 포트 반환 타입으로 일급 컬렉션 `Posts` 사용
- `category`: 카테고리별 필터링
- `page`: 0-based 페이지 번호

### 3.6 Domain Application - PostQueryService

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/application/PostQueryService.kt`

```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostQueryService(
    private val postQueryRepository: PostQueryRepository,
) {
    fun find(category: PostCategory, page: Int): Posts {
        return postQueryRepository.find(category, page)
    }
}
```

- Service는 조합만 담당. 현재 목록 조회는 추가 비즈니스 로직이 없으므로 Repository에 위임만 한다
- `@Transactional(readOnly = true)`: 읽기 전용 트랜잭션으로 성능 최적화

### 3.7 Infrastructure - PostTable (Exposed Table)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/entity/table/PostTable.kt`

```kotlin
package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object PostTable : BaseTable("COMMUNITY_POSTS", "COMMUNITY_POST_ID") {
    val authorEmail = varchar("AUTHOR_EMAIL", 255)
    val authorNickname = varchar("AUTHOR_NICKNAME", 255)
    val category = varchar("CATEGORY", 32)
    val title = varchar("TITLE", 100)
    val content = text("CONTENT")
    val likes = integer("LIKES").clientDefault { 0 }
    val comments = integer("COMMENTS").clientDefault { 0 }
}
```

- `BaseTable` 상속으로 공통 컬럼(createdDate, deleted 등) 자동 포함
- `category`는 `varchar`로 저장, 도메인에서 enum 변환 (기존 패턴과 동일 - MatchingResultTable의 패턴 참조)
- `content`는 `text()` 타입으로 길이 제한 없음

### 3.8 Infrastructure - PostEntity

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/entity/PostEntity.kt`

```kotlin
package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class PostEntity(
    val id: Long,
    val authorEmail: String,
    val authorNickname: String,
    val category: PostCategory,
    val title: String,
    val content: String,
    val likes: Int,
    val comments: Int,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Post {
        return Post(
            id = id,
            authorEmail = authorEmail,
            authorNickname = authorNickname,
            category = category,
            title = title,
            content = content,
            likes = likes,
            comments = comments,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): PostEntity {
            return PostEntity(
                id = row[PostTable.id].value,
                authorEmail = row[PostTable.authorEmail],
                authorNickname = row[PostTable.authorNickname],
                category = PostCategory.valueOf(row[PostTable.category]),
                title = row[PostTable.title],
                content = row[PostTable.content],
                likes = row[PostTable.likes],
                comments = row[PostTable.comments],
                createdDate = row[PostTable.createdDate],
            )
        }
    }
}
```

- `MatchingResultEntity`와 동일한 패턴: `companion object { from(row) }` + `toDomain()`
- `PostCategory.valueOf()`로 DB 문자열을 enum 변환

### 3.9 Infrastructure - PostQueryDao

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/PostQueryDao.kt`

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.PostEntity
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class PostQueryDao {
    fun find(category: String, pageSize: Int, offset: Long): List<PostEntity> {
        return PostTable
            .selectAll()
            .where {
                (PostTable.category eq category) and
                    (PostTable.deleted eq false)
            }
            .orderBy(PostTable.id to SortOrder.DESC)
            .limit(pageSize)
            .offset(offset)
            .map { row -> PostEntity.from(row) }
    }

    fun count(category: String): Long {
        return PostTable
            .selectAll()
            .where {
                (PostTable.category eq category) and
                    (PostTable.deleted eq false)
            }
            .count()
    }
}
```

- `category`는 `String`으로 받음: DAO는 인프라 레이어이므로 DB 컬럼 타입(String)과 일치시킴. enum 변환은 Repository에서 처리
- `offset`: `Long` 타입. Exposed의 `.offset()` 메서드가 `Long`을 받음
- `orderBy(PostTable.id to SortOrder.DESC)`: 최신 게시글이 위로 오도록 내림차순 정렬
- `count()`: 별도 메서드로 분리. 페이징 메타데이터 계산에 사용

### 3.10 Infrastructure - PostQueryCoreRepository

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/repository/PostQueryCoreRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.PostQueryDao
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Repository

@Repository
class PostQueryCoreRepository(
    private val postQueryDao: PostQueryDao,
) : PostQueryRepository {
    override fun find(category: PostCategory, page: Int): Posts {
        val offset = page.toLong() * Posts.PAGE_SIZE
        val entities = postQueryDao.find(category.name, Posts.PAGE_SIZE, offset)
        val totalCount = postQueryDao.count(category.name)

        return Posts(
            data = entities.map { it.toDomain() },
            totalCount = totalCount,
            currentPage = page,
        )
    }
}
```

- `category.name`: enum을 DB 저장 형태(String)로 변환
- `Posts.PAGE_SIZE`: 일급 컬렉션의 상수 사용으로 페이지 크기 일관성 보장
- `offset` 계산: `page * PAGE_SIZE`
- `entity.toDomain()`: Entity에서 도메인 객체로 변환 (기존 패턴 준수)

### 3.11 Infrastructure - EntityType 확장

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/exception/EntityType.kt`

```kotlin
package com.konkuk.ma.exception

enum class EntityType(val entityName: String, val keyName: String) {
    MEMBER("Member", "email"),
    MATCHING_RESULT("MatchingResult", "id"),
    REFRESH_TOKEN("RefreshToken", "email"),
    COMMUNITY_POST("CommunityPost", "id"),  // 추가
}
```

- 향후 게시글 상세 조회 등에서 `EntityNotFoundException`에 사용

### 3.12 Domain - ObfuscationType 확장

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/common/domain/id/ObfuscationType.kt`

```kotlin
package com.konkuk.ma.domain.common.domain.id

enum class ObfuscationType(val saltSuffix: String) {
    MEMBER("member"),
    TARGET_INFO("target-info"),
    MEMBER_PHOTO("member-photo"),
    MATCHING_RESULT("matching-result"),
    COMMUNITY_POST("community-post"),  // 추가
}
```

- 게시글 ID 난독화를 위한 타입 추가. 응답의 `id` 필드에 `@EncryptId`로 사용

### 3.13 Boot - PostResponse (응답 DTO)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/response/PostResponse.kt`

```kotlin
package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.support.id.EncryptId

class PostResponse(
    @EncryptId(ObfuscationType.COMMUNITY_POST)
    val id: Long,
    val nickname: String,
    val category: String,
    val title: String,
    val content: String,
    val likes: Int,
    val comments: Int,
    val timeAgo: String,
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id,
                nickname = post.authorNickname,
                category = post.category.name,
                title = post.title,
                content = post.content,
                likes = post.likes,
                comments = post.comments,
                timeAgo = post.calculateTimeAgo(),
            )
        }
    }
}
```

- `@EncryptId(ObfuscationType.COMMUNITY_POST)`: 게시글 ID 난독화
- `timeAgo`: 도메인 객체의 `calculateTimeAgo()` 호출로 경과 시간 문자열 생성
- api-todo 명세의 응답 필드와 1:1 매핑

### 3.14 Boot - PostsResponse (목록 응답 DTO)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/response/PostsResponse.kt`

```kotlin
package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.Posts

class PostsResponse(
    val posts: List<PostResponse>,
    val totalCount: Long,
    val currentPage: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(posts: Posts): PostsResponse {
            return PostsResponse(
                posts = posts.data.map { PostResponse.from(it) },
                totalCount = posts.totalCount,
                currentPage = posts.currentPage,
                hasNext = posts.hasNext(),
            )
        }
    }
}
```

- api-todo 명세의 `posts` 배열 + 페이징 메타데이터
- `hasNext`: 프론트엔드 "더보기" 표시에 활용

### 3.15 Boot - CommunityPostQueryApi (컨트롤러)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/CommunityPostQueryApi.kt`

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.response.PostsResponse
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class CommunityPostQueryApi(
    private val postQueryService: PostQueryService,
) {
    @GetMapping
    fun findPosts(
        @AuthenticationPrincipal email: String,
        @RequestParam category: PostCategory,
        @RequestParam(defaultValue = "0") page: Int,
    ): PostsResponse {
        val posts = postQueryService.find(category, page)
        return PostsResponse.from(posts)
    }
}
```

- `@AuthenticationPrincipal email`: 인증 필요 (SecurityConfig의 `anyRequest().authenticated()` 적용)
- `@RequestParam category: PostCategory`: Spring이 자동으로 문자열을 enum 변환. 잘못된 값이 들어오면 400 Bad Request
- `@RequestParam(defaultValue = "0") page`: 페이지 미지정 시 첫 페이지
- 기존 `MatchingResultQueryApi` 패턴과 동일한 구조

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `infrastructure/storage/ma-db-core/src/main/resources/script/ddl.sql` | 수정 | COMMUNITY_POSTS 테이블 DDL 추가 |
| 2 | `domain/ma-domain-core/.../community/domain/PostCategory.kt` | 신규 | 게시글 카테고리 enum |
| 3 | `domain/ma-domain-core/.../community/domain/Post.kt` | 신규 | 게시글 도메인 모델 + timeAgo 계산 |
| 4 | `domain/ma-domain-core/.../community/domain/Posts.kt` | 신규 | 일급 컬렉션 + 페이징 메타데이터 |
| 5 | `domain/ma-domain-core/.../community/domain/port/PostQueryRepository.kt` | 신규 | 조회 포트 인터페이스 |
| 6 | `domain/ma-domain-core/.../community/application/PostQueryService.kt` | 신규 | 조회 서비스 |
| 7 | `domain/ma-domain-core/.../exception/EntityType.kt` | 수정 | COMMUNITY_POST 추가 |
| 8 | `domain/ma-domain-core/.../common/domain/id/ObfuscationType.kt` | 수정 | COMMUNITY_POST 추가 |
| 9 | `infrastructure/storage/ma-db-core/.../community/entity/table/PostTable.kt` | 신규 | Exposed Table 정의 |
| 10 | `infrastructure/storage/ma-db-core/.../community/entity/PostEntity.kt` | 신규 | Entity + toDomain() + from() |
| 11 | `infrastructure/storage/ma-db-core/.../community/dao/PostQueryDao.kt` | 신규 | 페이징 조회 + count DAO |
| 12 | `infrastructure/storage/ma-db-core/.../community/repository/PostQueryCoreRepository.kt` | 신규 | 포트 구현체 |
| 13 | `boot/ma-boot-web/.../community/api/response/PostResponse.kt` | 신규 | 단건 응답 DTO |
| 14 | `boot/ma-boot-web/.../community/api/response/PostsResponse.kt` | 신규 | 목록 응답 DTO |
| 15 | `boot/ma-boot-web/.../community/api/CommunityPostQueryApi.kt` | 신규 | 컨트롤러 |

## 5. 고려사항

- **페이징 방식**: api-todo에서 `page` 쿼리 파라미터를 명시하고 있으므로 Offset 기반 페이징을 채택. 현재 커뮤니티 초기 단계에서 게시글 수가 적어 Offset 방식으로 충분하다. 대용량 전환 시 NoOffset(cursor) 방식으로 변경 가능하며, 이 경우 `Posts` 일급 컬렉션의 인터페이스만 수정하면 된다
- **비정규화 (AUTHOR_NICKNAME, LIKES, COMMENTS)**: 목록 조회 시 JOIN을 피하기 위해 비정규화. 닉네임 변경 기능이 현재 없으므로 정합성 문제 없음. 좋아요/댓글 수는 해당 기능 구현 시 증감 처리 필요
- **FK 미사용**: 프로젝트 규칙에 따라 FK 제약조건 없이 INDEX만 사용. AUTHOR_EMAIL의 참조 무결성은 애플리케이션 레벨에서 보장 (인증된 사용자만 게시글 작성 가능)
- **SecurityConfig 변경 불필요**: `anyRequest().authenticated()`에 의해 `/api/community/posts`는 자동으로 인증 필요. 별도 permit 설정 불필요
- **카테고리 enum 변환 에러**: 잘못된 category 값이 들어오면 Spring이 `MethodArgumentTypeMismatchException`을 발생시킴. 기존 글로벌 예외 핸들러에서 400 응답으로 처리됨
- **timeAgo 계산 위치**: 도메인 객체(`Post.calculateTimeAgo()`)에서 계산. 응답 DTO 생성 시점의 시간 기준으로 경과 시간을 산출하므로 정확도 충분
- **content 길이**: 목록 조회 시에도 content 전체를 반환 (api-todo 명세 기준). 프론트엔드에서 truncation 처리. 향후 성능 이슈 시 목록용 별도 DAO 메서드(content 미포함)를 추가할 수 있음
- **테스트**: PostQueryService, PostQueryDao, CommunityPostQueryApi 각각에 대해 KoTest + Mockk 테스트 작성 필요. 특히 `Post.calculateTimeAgo()`는 단위 테스트로 다양한 시간 케이스를 검증해야 함
