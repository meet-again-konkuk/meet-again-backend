package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
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
 * 댓글 상세 조회 E2E 통합 테스트.
 *
 * 좋아요 카운터 컬럼 제거 → 좋아요 행 COUNT 도출 리팩터 이후,
 * GET /api/community/comments/{commentId} 응답에서 루트 댓글과 대댓글의 `likes` 값이
 * 실제 좋아요 행 수와 항상 일치하는지 API → Service → DB 관통으로 검증한다.
 *
 * 핵심 검증: 루트/대댓글 likes 실제값, 좋아요 0인 케이스는 0.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentQueryIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    val viewerEmail = "comment-query-viewer@example.com"
    val viewerPassword = "password123"

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, CommentTable, CommentLikeTable, BlockTable)
        }
    }

    afterEach {
        transaction {
            BlockTable.deleteAll()
            CommentLikeTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(BlockTable, CommentLikeTable, CommentTable, PostTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(email: String, nickname: String = "작성자-$email", rawPassword: String? = null): Long {
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

    fun insertPost(authorId: Long = 1L): Long {
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
        authorId: Long = 1L,
        parentCommentId: Long? = null,
    ): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
                it[CommentTable.parentCommentId] = parentCommentId
            }.value
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

    fun blockMember(blockerId: Long, blockedId: Long) {
        transaction {
            BlockTable.insert {
                it[BlockTable.blockerId] = blockerId
                it[BlockTable.blockedId] = blockedId
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

    context("GET /api/community/comments/{commentId}") {

        test("루트 댓글과 대댓글의 likes가 실제 좋아요 행 수와 일치한다") {
            // Given - 루트 댓글(좋아요 3) + 대댓글 2개(좋아요 1, 5)
            val (_, accessToken) = loginAsViewer()
            insertMember(email = "author@example.com")
            val postId = insertPost()
            val rootCommentId = insertComment(postId)
            val firstReplyId = insertComment(postId, parentCommentId = rootCommentId)
            val secondReplyId = insertComment(postId, parentCommentId = rootCommentId)

            addCommentLikes(rootCommentId, 3)
            addCommentLikes(firstReplyId, 1)
            addCommentLikes(secondReplyId, 5)

            // When
            val result = mockMvc.getJson("/api/community/comments/{commentId}", rootCommentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 루트 댓글 likes
            val comment = mapper.readTree(result.response.contentAsString)
            comment.get("id").asLong() shouldBe rootCommentId
            comment.get("likes").asInt() shouldBe 3

            // Then - 각 대댓글 likes
            val replies = comment.get("replies")
            val firstReply = replies.first { it.get("id").asLong() == firstReplyId }
            val secondReply = replies.first { it.get("id").asLong() == secondReplyId }
            firstReply.get("likes").asInt() shouldBe 1
            secondReply.get("likes").asInt() shouldBe 5
        }

        test("좋아요가 0인 루트 댓글과 대댓글은 likes가 0이다") {
            // Given - 좋아요를 전혀 추가하지 않음
            val (_, accessToken) = loginAsViewer()
            insertMember(email = "author@example.com")
            val postId = insertPost()
            val rootCommentId = insertComment(postId)
            val replyId = insertComment(postId, parentCommentId = rootCommentId)

            // When
            val result = mockMvc.getJson("/api/community/comments/{commentId}", rootCommentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val comment = mapper.readTree(result.response.contentAsString)
            comment.get("likes").asInt() shouldBe 0
            val reply = comment.get("replies").first { it.get("id").asLong() == replyId }
            reply.get("likes").asInt() shouldBe 0
        }

        test("대댓글이 없는 루트 댓글의 likes는 실제 행 수와 일치하고 replies는 비어 있다") {
            // Given
            val (_, accessToken) = loginAsViewer()
            insertMember(email = "author@example.com")
            val postId = insertPost()
            val rootCommentId = insertComment(postId)
            addCommentLikes(rootCommentId, 2)

            // When
            val result = mockMvc.getJson("/api/community/comments/{commentId}", rootCommentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val comment = mapper.readTree(result.response.contentAsString)
            comment.get("likes").asInt() shouldBe 2
            comment.get("replies").size() shouldBe 0
        }
    }

    context("GET /api/community/comments/{commentId} (상태 필드)") {

        test("내가 작성하고 좋아요한 루트 댓글은 isMine·likedByMe=true, 타인 루트 댓글은 둘 다 false 다") {
            // Given
            val (viewerId, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "comment-author@example.com")
            val postId = insertPost(authorId)
            val myRoot = insertComment(postId, viewerId)
            val othersRoot = insertComment(postId, authorId)
            likeComment(myRoot, viewerId)

            // When & Then - 내 루트 댓글
            val mine = mockMvc.getJson("/api/community/comments/{commentId}", myRoot) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            val mineNode = mapper.readTree(mine.response.contentAsString)
            mineNode.get("isMine").asBoolean().shouldBeTrue()
            mineNode.get("likedByMe").asBoolean().shouldBeTrue()

            // When & Then - 타인 루트 댓글
            val others = mockMvc.getJson("/api/community/comments/{commentId}", othersRoot) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            val othersNode = mapper.readTree(others.response.contentAsString)
            othersNode.get("isMine").asBoolean().shouldBeFalse()
            othersNode.get("likedByMe").asBoolean().shouldBeFalse()
        }

        test("대댓글도 작성자/좋아요 여부에 따라 isMine·likedByMe가 채워진다") {
            // Given - 내가 쓴 대댓글(내가 좋아요) + 타인 대댓글
            val (viewerId, accessToken) = loginAsViewer()
            val authorId = insertMember(email = "comment-author@example.com")
            val postId = insertPost(authorId)
            val rootCommentId = insertComment(postId, authorId)
            val myReply = insertComment(postId, viewerId, parentCommentId = rootCommentId)
            val othersReply = insertComment(postId, authorId, parentCommentId = rootCommentId)
            likeComment(myReply, viewerId)

            // When
            val result = mockMvc.getJson("/api/community/comments/{commentId}", rootCommentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val replies = mapper.readTree(result.response.contentAsString).get("replies")
            val mine = replies.first { it.get("id").asLong() == myReply }
            val others = replies.first { it.get("id").asLong() == othersReply }
            mine.get("isMine").asBoolean().shouldBeTrue()
            mine.get("likedByMe").asBoolean().shouldBeTrue()
            others.get("isMine").asBoolean().shouldBeFalse()
            others.get("likedByMe").asBoolean().shouldBeFalse()
        }
    }

    context("GET /api/community/comments/{commentId} (차단 필터)") {

        test("차단한 작성자의 루트 댓글은 placeholder로 노출되고 blockedAuthor=true다") {
            // Given - 조회자가 루트 댓글 작성자를 차단
            val (viewerId, accessToken) = loginAsViewer()
            val blockedAuthorId = insertMember(email = "blocked-author@example.com")
            val postId = insertPost(blockedAuthorId)
            val rootCommentId = insertComment(postId, blockedAuthorId)
            blockMember(blockerId = viewerId, blockedId = blockedAuthorId)

            // When
            val result = mockMvc.getJson("/api/community/comments/{commentId}", rootCommentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val root = mapper.readTree(result.response.contentAsByteArray)
            root.get("blockedAuthor").asBoolean().shouldBeTrue()
            root.get("content").asText() shouldBe "차단한 사용자의 댓글입니다."
        }

        test("차단한 작성자의 대댓글만 placeholder이고 루트·미차단 대댓글은 원문으로 노출된다") {
            // Given - 루트 작성자는 차단하지 않고 대댓글 작성자 한 명만 차단
            val (viewerId, accessToken) = loginAsViewer()
            val rootAuthorId = insertMember(email = "root-author@example.com")
            val blockedReplyAuthorId = insertMember(email = "blocked-replier@example.com")
            val normalReplyAuthorId = insertMember(email = "normal-replier@example.com")
            val postId = insertPost(rootAuthorId)
            val rootCommentId = insertComment(postId, rootAuthorId)
            val blockedReply = insertComment(postId, blockedReplyAuthorId, parentCommentId = rootCommentId)
            val normalReply = insertComment(postId, normalReplyAuthorId, parentCommentId = rootCommentId)
            blockMember(blockerId = viewerId, blockedId = blockedReplyAuthorId)

            // When
            val result = mockMvc.getJson("/api/community/comments/{commentId}", rootCommentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 루트는 정상, 차단 대댓글만 placeholder
            val root = mapper.readTree(result.response.contentAsByteArray)
            root.get("blockedAuthor").asBoolean().shouldBeFalse()
            val replies = root.get("replies")
            val blocked = replies.first { it.get("id").asLong() == blockedReply }
            val normal = replies.first { it.get("id").asLong() == normalReply }
            blocked.get("blockedAuthor").asBoolean().shouldBeTrue()
            blocked.get("content").asText() shouldBe "차단한 사용자의 댓글입니다."
            normal.get("blockedAuthor").asBoolean().shouldBeFalse()
            normal.get("content").asText() shouldBe "테스트 댓글"
        }
    }
})
