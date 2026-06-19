package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * 댓글 좋아요 E2E 통합 테스트.
 *
 * 비정규화 카운터 제거 → 좋아요 행(CommentLikeTable) COUNT 도출 리팩터를
 * API(POST/DELETE /api/community/comments/{commentId}/likes) → Service → DB까지 관통하여 검증한다.
 *
 * 핵심 검증: 여러 명 좋아요/취소 후에도 응답 likeCount가 실제 좋아요 행 수와 항상 일치한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentLikeIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, CommentTable, CommentLikeTable)
        }
    }

    afterEach {
        transaction {
            CommentLikeTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(CommentLikeTable, CommentTable, PostTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(
        email: String,
        rawPassword: String = "password123",
        nickname: String = "테스터",
    ) {
        transaction {
            MemberTable.insert {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[MemberTable.nickname] = nickname
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }
        }
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

    fun insertComment(postId: Long, authorId: Long = 1L): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
            }.value
        }
    }

    fun login(email: String, rawPassword: String = "password123"): String {
        val request = mapOf("email" to email, "password" to rawPassword)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()
        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    fun countActiveLikes(commentId: Long): Long {
        return transaction {
            CommentLikeTable
                .selectAll()
                .where { (CommentLikeTable.commentId eq commentId) and (CommentLikeTable.deleted eq false) }
                .count()
        }
    }

    context("POST /api/community/comments/{commentId}/likes") {

        test("로그인 후 좋아요를 누르면 likeCount=1을 반환하고 좋아요 행이 1개 생성된다") {
            // Given
            val email = "comment-like-e2e@example.com"
            insertMember(email = email)
            val accessToken = login(email)
            val postId = insertPost()
            val commentId = insertComment(postId)

            // When
            val result = mockMvc.postJson("/api/community/comments/{commentId}/likes", commentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then
            val response = mapper.readTree(result.response.contentAsString)
            response.get("liked").asBoolean() shouldBe true
            response.get("likeCount").asInt() shouldBe 1
            countActiveLikes(commentId) shouldBe 1L
        }

        test("서로 다른 회원 3명이 좋아요를 누르면 likeCount=3이고 좋아요 행도 3개다") {
            // Given
            val postId = insertPost()
            val commentId = insertComment(postId)
            val emails = listOf("cliker1@example.com", "cliker2@example.com", "cliker3@example.com")

            // When
            emails.forEachIndexed { index, email ->
                insertMember(email = email)
                val accessToken = login(email)

                val result = mockMvc.postJson("/api/community/comments/{commentId}/likes", commentId) {
                    authorization("Bearer $accessToken")
                }
                    .andExpect { status { isCreated() } }
                    .andReturn()

                val likeCount = mapper.readTree(result.response.contentAsString).get("likeCount").asInt()
                likeCount shouldBe (index + 1)
            }

            // Then
            countActiveLikes(commentId) shouldBe emails.size.toLong()
        }

        test("인증 토큰 없이 좋아요를 누르면 401이 반환된다") {
            // Given
            val postId = insertPost()
            val commentId = insertComment(postId)

            // When & Then
            mockMvc.postJson("/api/community/comments/{commentId}/likes", commentId)
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/community/comments/{commentId}/likes") {

        test("좋아요를 취소하면 likeCount가 0으로 복귀하고 좋아요 행이 삭제된다") {
            // Given - 좋아요 누른 상태
            val email = "comment-unlike-e2e@example.com"
            insertMember(email = email)
            val accessToken = login(email)
            val postId = insertPost()
            val commentId = insertComment(postId)
            mockMvc.postJson("/api/community/comments/{commentId}/likes", commentId) {
                authorization("Bearer $accessToken")
            }.andExpect { status { isCreated() } }

            // When
            val result = mockMvc.deleteJson("/api/community/comments/{commentId}/likes", commentId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val response = mapper.readTree(result.response.contentAsString)
            response.get("liked").asBoolean() shouldBe false
            response.get("likeCount").asInt() shouldBe 0
            countActiveLikes(commentId) shouldBe 0L
        }

        test("3명 좋아요 후 1명이 취소하면 likeCount=2로 행 수와 정확히 일치한다") {
            // Given
            val postId = insertPost()
            val commentId = insertComment(postId)
            val emails = listOf("cmulti1@example.com", "cmulti2@example.com", "cmulti3@example.com")
            emails.forEach { email ->
                insertMember(email = email)
                val token = login(email)
                mockMvc.postJson("/api/community/comments/{commentId}/likes", commentId) {
                    authorization("Bearer $token")
                }.andExpect { status { isCreated() } }
            }

            // When
            val cancelToken = login(emails.first())
            val result = mockMvc.deleteJson("/api/community/comments/{commentId}/likes", commentId) {
                authorization("Bearer $cancelToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            mapper.readTree(result.response.contentAsString).get("likeCount").asInt() shouldBe 2
            countActiveLikes(commentId) shouldBe 2L
        }

        test("인증 토큰 없이 좋아요를 취소하면 401이 반환된다") {
            // Given
            val postId = insertPost()
            val commentId = insertComment(postId)

            // When & Then
            mockMvc.deleteJson("/api/community/comments/{commentId}/likes", commentId)
                .andExpect { status { isUnauthorized() } }
        }
    }
})
