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

    fun insertMember(email: String, nickname: String = "작성자", rawPassword: String? = null) {
        transaction {
            MemberTable.insert {
                it[MemberTable.email] = email
                it[password] = if (rawPassword != null) passwordEncoder.encode(rawPassword) else "encoded"
                it[MemberTable.nickname] = nickname
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }
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

    // 조회 엔드포인트도 인증이 필요하므로, 조회용 로그인 회원을 만들고 토큰을 반환한다.
    fun loginAsViewer(): String {
        insertMember(email = viewerEmail, nickname = "조회자", rawPassword = viewerPassword)
        return login(viewerEmail, viewerPassword)
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
            val accessToken = loginAsViewer()
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
            val accessToken = loginAsViewer()
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
            val accessToken = loginAsViewer()
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
})
