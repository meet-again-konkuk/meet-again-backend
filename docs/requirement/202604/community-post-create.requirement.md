# Design: 게시글 작성

> 작성일: 2026-04-06
> 상태: Draft

## 1. 설계 개요

인증된 사용자가 커뮤니티에 카테고리/제목/내용을 포함한 새 게시글을 작성하는 POST API를 구현한다. 기존 커뮤니티 도메인(Post, PostCategory, PostTable)을 활용하여 Command 계열 파일들을 추가한다.

---

## 2. 아키텍처

```
┌──────────────────────────────────────────────────────┐
│ boot/ma-boot-web                                     │
│                                                      │
│  CommunityPostCommandApi                             │
│    POST /api/community/posts                         │
│    @ResponseStatus(201 Created)                      │
│    └── NewPostRequest → NewPost 변환                  │
│    └── PostCommandService.create(newPost) 호출        │
│    └── NewPostResponse 반환                           │
└───────────────────────┬──────────────────────────────┘
                        │ (port)
┌───────────────────────▼──────────────────────────────┐
│ domain/ma-domain-core                                │
│                                                      │
│  PostCommandService                                  │
│    + create(newPost: NewPost): Long                   │
│    └── MemberQueryRepository.findOne(email): Member   │
│    └── NewPost에 닉네임 설정                           │
│    └── PostCommandRepository.save(newPost): Long      │
│                                                      │
│  PostCommandRepository (port)                        │
│    + save(newPost: NewPost): Long                     │
│                                                      │
│  NewPost (도메인 모델)                                │
│    - authorEmail, authorNickname, category,           │
│      title, content                                   │
│    + validateTitle()                                  │
└───────────────────────┬──────────────────────────────┘
                        │ (implements)
┌───────────────────────▼──────────────────────────────┐
│ infrastructure/storage/ma-db-core                    │
│                                                      │
│  PostCommandCoreRepository → PostCommandDao           │
│    └── PostTable.insertAndGetId { ... }               │
└──────────────────────────────────────────────────────┘
```

---

## 3. 상세 설계

### 3.1 Domain - NewPost (신규 도메인 모델)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/NewPost.kt`

```kotlin
package com.konkuk.ma.domain.community.domain

class NewPost(
    val authorEmail: String,
    val authorNickname: String,
    val category: PostCategory,
    val title: String,
    val content: String,
) {
    init {
        validateTitle()
        validateContent()
    }

    private fun validateTitle() {
        require(title.isNotBlank()) { "게시글 제목은 비어있을 수 없습니다." }
        require(title.length <= MAX_TITLE_LENGTH) { "게시글 제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다." }
    }

    private fun validateContent() {
        require(content.isNotBlank()) { "게시글 내용은 비어있을 수 없습니다." }
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 40
    }
}
```

- `NewPost`는 생성 전용 도메인 모델. 기존 `Post`는 조회용(id, likes, comments, createdDate 포함)이므로 분리한다
- `authorNickname`은 Service에서 `MemberQueryRepository`를 통해 조회한 Member의 nickname을 주입한다
- `validateTitle()`, `validateContent()`: 도메인 객체 내부에서 상태 검증 (OOP 원칙: 상태 검증은 객체 내부에서)
- `MAX_TITLE_LENGTH = 40`: api-todo.md 스펙에 따라 40자 제한

### 3.2 Domain Port - PostCommandRepository (신규 포트)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/domain/port/PostCommandRepository.kt`

```kotlin
package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.NewPost

interface PostCommandRepository {
    fun save(newPost: NewPost): Long
}
```

- 반환값 `Long`: 생성된 게시글의 ID
- 파라미터로 도메인 객체 `NewPost` 사용 (포트 인터페이스는 도메인 타입 사용 규칙)

### 3.3 Domain Service - PostCommandService (신규 서비스)

**파일**: `domain/ma-domain-core/src/main/kotlin/com/konkuk/ma/domain/community/application/PostCommandService.kt`

```kotlin
package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostCommandService(
    private val postCommandRepository: PostCommandRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun create(email: String, category: PostCategory, title: String, content: String): Long {
        val member = memberQueryRepository.findOne(email)
        val newPost = NewPost(
            authorEmail = email,
            authorNickname = member.nickname,
            category = category,
            title = title,
            content = content,
        )
        return postCommandRepository.save(newPost)
    }
}
```

- Service는 조합만 담당: Member 조회 -> NewPost 생성(검증 포함) -> 저장
- `MemberQueryRepository.findOne(email)`: 기존 포트, non-null 반환 (존재하지 않으면 Repository에서 예외)
- `member.nickname`으로 작성자 닉네임을 가져온다. DB에 닉네임을 저장하는 이유는 PostTable의 기존 설계를 따르는 것
- 파라미터로 원시 타입을 받고 Service 내부에서 NewPost를 생성하는 이유: Controller에서 닉네임을 알 수 없으므로 Service에서 조립

### 3.4 Infrastructure - PostCommandDao (신규 DAO)

**파일**: `infrastructure/storage/ma-db-core/src/main/kotlin/com/konkuk/ma/domain/community/dao/PostCommandDao.kt`

```kotlin
package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.entity.table.PostTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class PostCommandDao {
    fun save(newPost: NewPost): Long {
        return PostTable.insertAndGetId {
            it[authorEmail] = newPost.authorEmail
            it[authorNickname] = newPost.authorNickname
            it[category] = newPost.category.name
            it[title] = newPost.title
            it[content] = newPost.content
            it[createdBy] = newPost.authorEmail
            it[lastModifiedBy] = newPost.authorEmail
        }.value
    }
}
```

- `insertAndGetId`: Exposed DSL로 INSERT 후 자동 생성된 PK 반환
- `likes`, `comments`는 PostTable에서 `clientDefault { 0 }`으로 설정되어 있으므로 명시하지 않음
- `createdBy`, `lastModifiedBy`: 감사 컬럼에 작성자 이메일 설정 (기존 TargetInfoCommandDao 패턴 동일)

### 3.5 Infrastructure - PostCommandCoreRepository (신규 Repository)

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
    override fun save(newPost: NewPost): Long {
        return postCommandDao.save(newPost)
    }
}
```

- 포트 구현체가 DAO에 위임하는 기존 패턴과 동일

### 3.6 Boot - NewPostRequest (신규 Request DTO)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/request/NewPostRequest.kt`

```kotlin
package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.community.domain.PostCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

class NewPostRequest(
    @field:NotNull(message = "카테고리는 필수입니다.")
    val category: PostCategory?,

    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 40, message = "제목은 40자 이하여야 합니다.")
    val title: String?,

    @field:NotBlank(message = "내용은 필수입니다.")
    val content: String?,
)
```

- `category`를 `PostCategory?`(nullable)로 선언하고 `@NotNull`로 검증: 잘못된 enum 값이 오면 Spring의 HttpMessageNotReadableException이 발생하고, null이면 `@NotNull`로 검증
- `@Size(max = 40)`: Bean Validation으로 title 길이 1차 검증. NewPost 도메인 모델에서도 2차 검증
- 필드를 nullable로 선언하고 `@NotBlank`/`@NotNull`로 검증하는 이유: JSON에서 필드 자체가 누락된 경우 Kotlin non-null 필드는 역직렬화 시 예외가 발생하여 validation 메시지가 노출되지 않음

### 3.7 Boot - NewPostResponse (신규 Response DTO)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/response/NewPostResponse.kt`

```kotlin
package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.support.id.EncryptId

class NewPostResponse(
    @EncryptId(ObfuscationType.COMMUNITY_POST)
    val postId: Long,
)
```

- `@EncryptId(ObfuscationType.COMMUNITY_POST)`: 기존에 정의된 ObfuscationType 활용하여 ID 난독화
- 생성 응답은 ID만 반환 (기존 NewTargetInfoResponse 패턴 참고, 단 registerEmail은 불필요)

### 3.8 Boot - CommunityPostCommandApi (신규 Controller)

**파일**: `boot/ma-boot-web/src/main/kotlin/com/konkuk/ma/domain/community/api/CommunityPostCommandApi.kt`

```kotlin
package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.request.NewPostRequest
import com.konkuk.ma.domain.community.api.response.NewPostResponse
import com.konkuk.ma.domain.community.application.PostCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts")
class CommunityPostCommandApi(
    private val postCommandService: PostCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: NewPostRequest,
    ): NewPostResponse {
        val postId = postCommandService.create(
            email = email,
            category = request.category!!,
            title = request.title!!,
            content = request.content!!,
        )
        return NewPostResponse(postId = postId)
    }
}
```

- `@ResponseStatus(HttpStatus.CREATED)`: POST 리소스 생성 -> 201 Created (RESTful 규칙)
- `@RequestMapping("/api/community/posts")`: 기존 CommunityPostQueryApi와 동일한 base path
- `request.category!!`: `@Valid`에서 `@NotNull` 검증을 통과한 후이므로 non-null 단언이 안전
- Controller는 Request -> 서비스 호출 -> Response 변환만 담당 (비즈니스 로직 없음)

---

## 4. 구현 순서

| # | 파일 | 변경 유형 | 내용 |
|---|------|-----------|------|
| 1 | `domain/ma-domain-core/.../community/domain/NewPost.kt` | 신규 | 게시글 생성 도메인 모델 (title/content 검증 포함) |
| 2 | `domain/ma-domain-core/.../community/domain/port/PostCommandRepository.kt` | 신규 | 게시글 저장 포트 인터페이스 |
| 3 | `domain/ma-domain-core/.../community/application/PostCommandService.kt` | 신규 | 게시글 생성 서비스 (Member 조회 + NewPost 생성 + 저장) |
| 4 | `infrastructure/storage/ma-db-core/.../community/dao/PostCommandDao.kt` | 신규 | Exposed insertAndGetId로 게시글 INSERT |
| 5 | `infrastructure/storage/ma-db-core/.../community/repository/PostCommandCoreRepository.kt` | 신규 | 포트 구현체, DAO에 위임 |
| 6 | `boot/ma-boot-web/.../community/api/request/NewPostRequest.kt` | 신규 | Request DTO (Bean Validation) |
| 7 | `boot/ma-boot-web/.../community/api/response/NewPostResponse.kt` | 신규 | Response DTO (ID 난독화) |
| 8 | `boot/ma-boot-web/.../community/api/CommunityPostCommandApi.kt` | 신규 | POST /api/community/posts 컨트롤러 |

---

## 5. 고려사항

- **DDL 변경 없음**: 기존 `COMMUNITY_POSTS` 테이블 구조가 게시글 작성에 필요한 모든 컬럼을 이미 포함하고 있으므로 DDL 변경이 불필요하다.

- **닉네임 저장 전략**: PostTable에 `AUTHOR_NICKNAME` 컬럼이 이미 존재하며, 조회 시 JOIN 없이 바로 닉네임을 표시하기 위한 비정규화 설계이다. 사용자가 닉네임을 변경하면 기존 게시글의 닉네임은 변경되지 않는다는 점을 인지해야 한다. 닉네임 변경 시 기존 게시글 업데이트가 필요하면 별도 이슈로 처리한다.

- **Validation 이중 방어**: Request DTO에서 Bean Validation(`@NotBlank`, `@Size`)으로 1차 검증, NewPost 도메인 모델의 `init` 블록에서 2차 검증한다. Request Validation은 사용자 입력 형식 검증, 도메인 Validation은 비즈니스 규칙 검증의 역할을 분리한다.

- **Controller 분리**: 기존 `CommunityPostQueryApi`(GET)와 별도로 `CommunityPostCommandApi`(POST)를 만든다. 프로젝트의 기존 패턴(MatchingResultQueryApi / MatchingResultCommandApi 분리)과 일관성을 유지한다.

- **title 최대 길이**: api-todo.md 스펙에서 40자로 정의되어 있으며, PostTable의 `TITLE` 컬럼은 `VARCHAR(100)`이다. DB 컬럼 길이보다 도메인 제약이 더 엄격하므로 DB 레벨에서는 문제 없다.

- **content 최대 길이**: api-todo.md에서 content 최대 길이가 명시되지 않았다. PostTable의 `CONTENT` 컬럼이 `TEXT` 타입이므로 DB 레벨에서는 제한이 거의 없다. 필요 시 도메인 모델에 최대 길이 제약을 추가할 수 있다.
