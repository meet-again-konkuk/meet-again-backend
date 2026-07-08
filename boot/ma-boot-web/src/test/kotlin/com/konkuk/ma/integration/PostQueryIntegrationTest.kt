package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostImageTable
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * 게시글 조회 E2E 통합 테스트.
 *
 * 좋아요 카운터 컬럼 제거 → 좋아요 행 COUNT 도출 리팩터 이후,
 * GET /api/community/posts (목록) 과 GET /api/community/posts/{id} (상세) 응답의 `likes` 값이
 * 실제 좋아요 행 수와 항상 일치하는지 API → Service → DB 관통으로 검증한다.
 *
 * 핵심 검증: 좋아요 0인 글은 0, N개인 글은 N. 글마다 서로 다른 좋아요 수가 섞여 있어도 정확히 도출된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostQueryIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    val viewerEmail = "post-query-viewer@example.com"
    val viewerPassword = "password123"

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, CommentTable, PostLikeTable, CommentLikeTable, PostImageTable, BlockTable)
        }
    }

    afterEach {
        transaction {
            PostImageTable.deleteAll()
            BlockTable.deleteAll()
            PostLikeTable.deleteAll()
            CommentLikeTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(PostImageTable, BlockTable, PostLikeTable, CommentLikeTable, CommentTable, PostTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(email: String, nickname: String = "작성자", rawPassword: String? = null): Long {
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = email
                it[password] = if (rawPassword != null) passwordEncoder.encode(rawPassword) else "encoded"
                it[MemberTable.nickname] = nickname
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun login(email: String, rawPassword: String): String {
        val request = mapOf("email" to email, "password" to rawPassword)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()
        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    // 조회 엔드포인트도 인증이 필요하므로, 조회용 로그인 회원을 만들고 (memberId, accessToken) 을 반환한다.
    fun loginAsViewer(): Pair<Long, String> {
        val viewerId = insertMember(email = viewerEmail, nickname = "조회자", rawPassword = viewerPassword)
        return viewerId to login(viewerEmail, viewerPassword)
    }

    fun insertPost(authorId: Long): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[category] = "CHEER"
                it[title] = "테스트 게시글"
                it[content] = "내용"
            }.value
        }
    }

    fun insertComment(
        postId: Long,
        authorId: Long,
        parentCommentId: Long? = null,
        deleted: Boolean = false,
    ): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
                it[CommentTable.parentCommentId] = parentCommentId
                it[CommentTable.deleted] = deleted
            }.value
        }
    }

    fun likePost(postId: Long, memberId: Long, deleted: Boolean = false) {
        transaction {
            PostLikeTable.insert {
                it[PostLikeTable.postId] = postId
                it[PostLikeTable.memberId] = memberId
                it[PostLikeTable.deleted] = deleted
            }
        }
    }

    fun blockMember(blockerId: Long, blockedId: Long, deleted: Boolean = false) {
        transaction {
            BlockTable.insert {
                it[BlockTable.blockerId] = blockerId
                it[BlockTable.blockedId] = blockedId
                it[BlockTable.deleted] = deleted
            }
        }
    }

    fun addPostLikes(postId: Long, count: Int) {
        transaction {
            repeat(count) { i ->
                PostLikeTable.insert {
                    it[PostLikeTable.postId] = postId
                    it[memberId] = postId * 1000L + i
                }
            }
        }
    }

    fun addCommentLikes(commentId: Long, count: Int) {
        transaction {
            repeat(count) { i ->
                CommentLikeTable.insert {
                    it[CommentLikeTable.commentId] = commentId
                    it[memberId] = commentId * 1000L + i
                }
            }
        }
    }

    fun likeComment(commentId: Long, memberId: Long) {
        transaction {
            CommentLikeTable.insert {
                it[CommentLikeTable.commentId] = commentId
                it[CommentLikeTable.memberId] = memberId
            }
        }
    }

    fun findPostNode(posts: JsonNode, postId: Long): JsonNode {
        return posts.first { it.get("id").asLong() == postId }
    }

    fun insertPostImage(postId: Long) {
        transaction {
            PostImageTable.insert {
                it[PostImageTable.postId] = postId
                it[storageKey] = "community/post-image/$postId/photo.jpg"
                it[originalFilename] = "photo.jpg"
                it[mimeType] = "image/jpeg"
                it[fileSize] = 1024L
                it[thumbnailKey] = "community/thumbnail/$postId/thumb_photo.jpg"
            }
        }
    }

    context("GET /api/community/posts (목록)") {

        test("각 게시글의 likes는 실제 좋아요 행 수와 일치한다 - 0개와 N개가 섞인 경우") {
            // Given - 조회자 로그인 + 작성자 회원 + 게시글 3개, 좋아요 수는 각각 0, 2, 5
            val (_, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "author@example.com", nickname = "작성자")
            val postWithZero = insertPost(authorId)
            val postWithTwo = insertPost(authorId)
            val postWithFive = insertPost(authorId)
            addPostLikes(postWithTwo, 2)
            addPostLikes(postWithFive, 5)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 응답의 각 글 likes가 실제 좋아요 행 수와 정확히 일치
            val data = mapper.readTree(result.response.contentAsString).get("data")
            findPostNode(data, postWithZero).get("likes").asInt() shouldBe 0
            findPostNode(data, postWithTwo).get("likes").asInt() shouldBe 2
            findPostNode(data, postWithFive).get("likes").asInt() shouldBe 5
        }

        test("좋아요가 하나도 없으면 모든 게시글의 likes는 0이다") {
            // Given
            val (_, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "author@example.com")
            val postId = insertPost(authorId)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val data = mapper.readTree(result.response.contentAsString).get("data")
            findPostNode(data, postId).get("likes").asInt() shouldBe 0
        }

        test("게시글 작성자의 nickname이 응답에 반영된다") {
            // Given
            val (_, accessToken) = loginAsViewer()
            val nickname = "응원단장"
            val authorId = insertMember(email = "cheerleader@example.com", nickname = nickname)
            val postId = insertPost(authorId = authorId)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 한글 닉네임은 UTF-8 raw bytes로 파싱하여 charset 디코딩 문제를 피한다
            val data = mapper.readTree(result.response.contentAsByteArray).get("data")
            findPostNode(data, postId).get("nickname").asText() shouldBe nickname
        }
    }

    context("GET /api/community/posts/{id} (상세)") {

        test("상세 응답의 글 likes와 각 댓글 likes가 실제 좋아요 행 수와 일치한다") {
            // Given - 글 좋아요 3개, 댓글 2개에 각각 좋아요 1개/4개
            val (_, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "author@example.com")
            val postId = insertPost(authorId)
            addPostLikes(postId, 3)

            val firstCommentId = insertComment(postId, authorId)
            val secondCommentId = insertComment(postId, authorId)
            addCommentLikes(firstCommentId, 1)
            addCommentLikes(secondCommentId, 4)

            // When
            val result = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 글 likes
            val detail = mapper.readTree(result.response.contentAsString)
            detail.get("likes").asInt() shouldBe 3

            // Then - 각 댓글 likes
            val comments = detail.get("comments")
            val firstComment = comments.first { it.get("id").asLong() == firstCommentId }
            val secondComment = comments.first { it.get("id").asLong() == secondCommentId }
            firstComment.get("likes").asInt() shouldBe 1
            secondComment.get("likes").asInt() shouldBe 4
        }

        test("좋아요가 0인 글과 댓글은 likes가 0이다") {
            // Given - 좋아요를 전혀 추가하지 않음
            val (_, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "author@example.com")
            val postId = insertPost(authorId)
            val commentId = insertComment(postId, authorId)

            // When
            val result = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val detail = mapper.readTree(result.response.contentAsString)
            detail.get("likes").asInt() shouldBe 0
            val comment = detail.get("comments").first { it.get("id").asLong() == commentId }
            comment.get("likes").asInt() shouldBe 0
        }
    }

    context("이미지 첨부 게시글 조회 필드 (REQ-013)") {

        test("이미지가 있는 게시글은 목록에 thumbnailUrl, 상세에 imageUrl 이 담긴다") {
            // Given - active 이미지가 달린 게시글
            val (_, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "image-author@example.com")
            val postId = insertPost(authorId)
            insertPostImage(postId)

            // When - 목록
            val listResult = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            // When - 상세
            val detailResult = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 목록은 thumbnailUrl, 상세는 imageUrl 필드가 채워진다
            val listData = mapper.readTree(listResult.response.contentAsString).get("data")
            findPostNode(listData, postId).get("thumbnailUrl").asText()
                .startsWith("/files/community/thumbnail/$postId/") shouldBe true

            val detail = mapper.readTree(detailResult.response.contentAsString)
            detail.get("imageUrl").asText()
                .startsWith("/files/community/post-image/$postId/") shouldBe true
        }

        test("이미지가 없는 게시글은 목록·상세 응답에 imageUrl·thumbnailUrl 필드가 null로 포함된다") {
            // Given - 이미지 없는 게시글
            val (_, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "no-image-author@example.com")
            val postId = insertPost(authorId)

            // When
            val listResult = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            val detailResult = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 이미지가 없으면 필드는 응답에 포함되고 값은 null 이다
            val listNode = findPostNode(mapper.readTree(listResult.response.contentAsString).get("data"), postId)
            listNode.get("imageUrl").isNull shouldBe true
            listNode.get("thumbnailUrl").isNull shouldBe true

            val detail = mapper.readTree(detailResult.response.contentAsString)
            detail.get("imageUrl").isNull shouldBe true
            detail.get("thumbnailUrl").isNull shouldBe true
        }
    }

    context("GET /api/community/posts (목록 상태 필드)") {

        test("내가 활성 좋아요한 게시글만 likedByMe=true 이고 타인 좋아요·취소한 좋아요는 false 다") {
            // Given - 조회자가 A만 좋아요, B는 타인이 좋아요, C는 조회자가 눌렀다 취소(soft delete)
            val (viewerId, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "author@example.com")
            val likedPost = insertPost(authorId)
            val othersLikedPost = insertPost(authorId)
            val cancelledPost = insertPost(authorId)
            likePost(likedPost, viewerId)
            likePost(othersLikedPost, authorId)
            likePost(cancelledPost, viewerId, deleted = true)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val data = mapper.readTree(result.response.contentAsString).get("data")
            findPostNode(data, likedPost).get("likedByMe").asBoolean().shouldBeTrue()
            findPostNode(data, othersLikedPost).get("likedByMe").asBoolean().shouldBeFalse()
            findPostNode(data, cancelledPost).get("likedByMe").asBoolean().shouldBeFalse()
        }

        test("내가 작성한 게시글은 isMine=true, 타인 게시글은 isMine=false 다") {
            // Given
            val (viewerId, accessToken) = loginAsViewer()
            val otherId = insertMember(email = "other@example.com")
            val myPost = insertPost(viewerId)
            val othersPost = insertPost(otherId)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val data = mapper.readTree(result.response.contentAsString).get("data")
            findPostNode(data, myPost).get("isMine").asBoolean().shouldBeTrue()
            findPostNode(data, othersPost).get("isMine").asBoolean().shouldBeFalse()
        }

        test("commentCount는 soft-deleted를 제외한 루트+대댓글 수와 일치하고 댓글 없는 글은 0이다") {
            // Given - 활성 루트 2 + 활성 대댓글 1 + 삭제된 댓글 1, 그리고 댓글 없는 글 1
            val (_, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "author@example.com")
            val postId = insertPost(authorId)
            val emptyPostId = insertPost(authorId)
            val rootComment = insertComment(postId, authorId)
            insertComment(postId, authorId)
            insertComment(postId, authorId, parentCommentId = rootComment)
            insertComment(postId, authorId, deleted = true)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 삭제된 1건을 제외한 3건, 댓글 없는 글은 0
            val data = mapper.readTree(result.response.contentAsString).get("data")
            findPostNode(data, postId).get("commentCount").asInt() shouldBe 3
            findPostNode(data, emptyPostId).get("commentCount").asInt() shouldBe 0
        }

        test("게시글 30개를 한 번에 조회해도 각 게시글의 likedByMe·isMine·commentCount가 모두 정확하다 (N+1 없이 배치 조립 스케일 검증)") {
            // Given - 서로 다른 상태의 게시글 30개를 만들고 기대값을 함께 기록한다
            val (viewerId, accessToken) = loginAsViewer()
            val otherId = insertMember(email = "other@example.com")
            val expected = (0 until 30).associate { index ->
                val authorIsViewer = index % 2 == 0
                val postId = insertPost(if (authorIsViewer) viewerId else otherId)

                val viewerLikes = index % 3 == 0
                if (viewerLikes) likePost(postId, viewerId)
                likePost(postId, otherId) // 타인 좋아요는 likedByMe에 영향 없어야 한다

                val activeComments = index % 4
                repeat(activeComments) { insertComment(postId, otherId) }
                insertComment(postId, otherId, deleted = true) // 삭제 댓글은 commentCount에서 제외돼야 한다

                postId to Triple(authorIsViewer, viewerLikes, activeComments)
            }

            // When - 30개를 한 페이지로 조회
            val result = mockMvc.getJson("/api/community/posts?size=50") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 게시글 수와 상태 필드가 모두 기대값과 일치
            val data = mapper.readTree(result.response.contentAsString).get("data")
            data.size() shouldBe 30
            expected.forEach { (postId, spec) ->
                val (authorIsViewer, viewerLikes, activeComments) = spec
                val post = findPostNode(data, postId)
                post.get("isMine").asBoolean() shouldBe authorIsViewer
                post.get("likedByMe").asBoolean() shouldBe viewerLikes
                post.get("commentCount").asInt() shouldBe activeComments
            }
        }
    }

    context("GET /api/community/posts (목록 차단 필터)") {

        test("차단한 작성자의 게시글은 목록에서 제외되고 나머지 게시글의 정렬(id 내림차순)이 유지된다") {
            // Given - 비차단 작성자 게시글 3개 사이에 차단 작성자 게시글 1개가 섞여 있다
            val (viewerId, accessToken) = loginAsViewer()
            val blockedAuthorId = insertMember(email = "blocked-author@example.com")
            val visibleAuthorId = insertMember(email = "visible-author@example.com")
            val first = insertPost(visibleAuthorId)
            insertPost(blockedAuthorId)
            val second = insertPost(visibleAuthorId)
            val third = insertPost(visibleAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedAuthorId)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 차단 게시글만 빠지고 나머지 3개가 id 내림차순으로 유지된다
            val data = mapper.readTree(result.response.contentAsString).get("data")
            data.map { it.get("id").asLong() } shouldBe listOf(third, second, first)
        }

        test("해제(soft delete)된 차단은 목록 필터에 영향을 주지 않아 해당 작성자 게시글이 노출된다") {
            // Given - 차단이 이미 해제된 상태
            val (viewerId, accessToken) = loginAsViewer()
            val unblockedAuthorId = insertMember(email = "unblocked-author@example.com")
            val visiblePost = insertPost(unblockedAuthorId)
            blockMember(blockerId = viewerId, blockedId = unblockedAuthorId, deleted = true)

            // When
            val result = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val data = mapper.readTree(result.response.contentAsString).get("data")
            data.map { it.get("id").asLong() } shouldContain visiblePost
        }
    }

    context("GET /api/community/posts/{id} (상세 상태 필드)") {

        test("내가 작성하고 좋아요한 글 상세는 isMine·likedByMe=true, 타인 글은 둘 다 false 다") {
            // Given
            val (viewerId, accessToken) = loginAsViewer()
            val otherId = insertMember(email = "other@example.com")
            val myPost = insertPost(viewerId)
            val othersPost = insertPost(otherId)
            likePost(myPost, viewerId)

            // When & Then - 내 글
            val mine = mockMvc.getJson("/api/community/posts/{id}", myPost) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            val mineDetail = mapper.readTree(mine.response.contentAsString)
            mineDetail.get("isMine").asBoolean().shouldBeTrue()
            mineDetail.get("likedByMe").asBoolean().shouldBeTrue()

            // When & Then - 타인 글
            val others = mockMvc.getJson("/api/community/posts/{id}", othersPost) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            val othersDetail = mapper.readTree(others.response.contentAsString)
            othersDetail.get("isMine").asBoolean().shouldBeFalse()
            othersDetail.get("likedByMe").asBoolean().shouldBeFalse()
        }

        test("상세의 각 댓글은 작성자/좋아요 여부에 따라 isMine·likedByMe가 채워진다") {
            // Given - 내가 쓴 댓글(내가 좋아요) + 타인 댓글
            val (viewerId, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "author@example.com")
            val postId = insertPost(authorId)
            val myComment = insertComment(postId, viewerId)
            val othersComment = insertComment(postId, authorId)
            likeComment(myComment, viewerId)

            // When
            val result = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val comments = mapper.readTree(result.response.contentAsString).get("comments")
            val mine = comments.first { it.get("id").asLong() == myComment }
            val others = comments.first { it.get("id").asLong() == othersComment }
            mine.get("isMine").asBoolean().shouldBeTrue()
            mine.get("likedByMe").asBoolean().shouldBeTrue()
            others.get("isMine").asBoolean().shouldBeFalse()
            others.get("likedByMe").asBoolean().shouldBeFalse()
        }

        test("soft-deleted 댓글도 placeholder로 노출되며 isMine은 실제 작성자 기준으로 유지된다") {
            // Given - 내가 쓴 뒤 삭제된 댓글
            val (viewerId, accessToken) = loginAsViewer()
            val postId = insertPost(viewerId)
            val deletedComment = insertComment(postId, viewerId, deleted = true)

            // When
            val result = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 내용은 placeholder이지만 isMine은 유지된다
            val comments = mapper.readTree(result.response.contentAsByteArray).get("comments")
            val placeholder = comments.first { it.get("id").asLong() == deletedComment }
            placeholder.get("content").asText() shouldBe "삭제된 댓글입니다."
            placeholder.get("isMine").asBoolean().shouldBeTrue()
        }
    }

    context("GET /api/community/posts/{id} (상세 차단 필터)") {

        test("차단한 작성자의 게시글을 상세 조회하면 404를 반환한다") {
            // Given - 조회자가 게시글 작성자를 차단
            val (viewerId, accessToken) = loginAsViewer()
            val blockedAuthorId = insertMember(email = "blocked-author@example.com")
            val postId = insertPost(blockedAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedAuthorId)

            // When & Then
            mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isNotFound() } }
        }

        test("차단한 작성자의 댓글은 placeholder와 blockedAuthor=true, 미차단 작성자의 댓글은 원문과 false로 노출된다") {
            // Given - 게시글 작성자는 차단하지 않고(상세 조회 가능) 댓글 작성자 한 명만 차단
            val (viewerId, accessToken) = loginAsViewer()
            val postAuthorId = insertMember(email = "post-author@example.com")
            val blockedCommentAuthorId = insertMember(email = "blocked-commenter@example.com")
            val normalCommentAuthorId = insertMember(email = "normal-commenter@example.com")
            val postId = insertPost(postAuthorId)
            val blockedComment = insertComment(postId, blockedCommentAuthorId)
            val normalComment = insertComment(postId, normalCommentAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedCommentAuthorId)

            // When
            val result = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val comments = mapper.readTree(result.response.contentAsByteArray).get("comments")
            val blocked = comments.first { it.get("id").asLong() == blockedComment }
            val normal = comments.first { it.get("id").asLong() == normalComment }
            blocked.get("blockedAuthor").asBoolean().shouldBeTrue()
            blocked.get("content").asText() shouldBe "차단한 사용자의 댓글입니다."
            normal.get("blockedAuthor").asBoolean().shouldBeFalse()
            normal.get("content").asText() shouldBe "테스트 댓글"
        }
    }
})
