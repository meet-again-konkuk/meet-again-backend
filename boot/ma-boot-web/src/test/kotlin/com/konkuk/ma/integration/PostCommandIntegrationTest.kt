package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.domain.PostDetails
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
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
 * 게시글 수정/삭제(REQ-012) API E2E 통합 테스트.
 *
 * PATCH/DELETE /api/community/posts/{postId} 를 실제 HTTP 요청으로 컨트롤러→Service→도메인→DB 까지 관통 검증한다.
 * - 정상: 본인 수정 200(+상세 재조회로 최신값 확인) / 본인 삭제 204(+상세 404·목록 제외)
 * - 권한/존재: 타인 403, 없는·삭제된 글 404, 반복 삭제 404
 * - 입력 검증: 빈/초과 제목·내용 400, 인증 없음 401
 * - 정책: 삭제된 게시글에 신규 댓글/좋아요 시 404
 *
 * REST Docs 스니펫은 별도 단계에서 다루므로 여기서는 기능만 검증한다.
 * 한국어 응답 필드는 charset 이슈로 contentAsByteArray 로 파싱한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostCommandIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, CommentTable, PostLikeTable, CommentLikeTable)
        }
    }

    afterEach {
        transaction {
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
            SchemaUtils.drop(PostLikeTable, CommentLikeTable, CommentTable, PostTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(email: String, rawPassword: String = "password123", nickname: String = "테스터"): Long {
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[MemberTable.nickname] = nickname
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertPost(authorId: Long): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[category] = "SUCCESS_STORY"
                it[title] = "원본 제목"
                it[content] = "원본 내용"
            }.value
        }
    }

    fun insertComment(postId: Long, authorId: Long) {
        transaction {
            CommentTable.insert {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
            }
        }
    }

    fun likePost(postId: Long, memberId: Long) {
        transaction {
            PostLikeTable.insert {
                it[PostLikeTable.postId] = postId
                it[PostLikeTable.memberId] = memberId
            }
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

    fun editRequest(
        category: String = "COUNSELING",
        title: String = "수정된 제목",
        content: String = "수정된 내용",
    ): String {
        return mapper.writeValueAsString(mapOf("category" to category, "title" to title, "content" to content))
    }

    context("PATCH /api/community/posts/{postId}") {

        test("본인이 수정하면 200과 {postId, updated:true}를 반환하고 상세 재조회 시 최신 값이 반영된다") {
            // Given
            val email = "patch-owner@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When
            val patchResult = mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
                content = editRequest(category = "COUNSELING", title = "수정된 제목", content = "수정된 내용")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 응답 본문
            val patchBody = mapper.readTree(patchResult.response.contentAsString)
            patchBody.get("postId").asLong() shouldBe postId
            patchBody.get("updated").asBoolean() shouldBe true

            // Then - 상세 재조회 시 최신 값 반영
            val getResult = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $token")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            val detail = mapper.readTree(getResult.response.contentAsByteArray)
            detail.get("category").asText() shouldBe "COUNSELING"
            detail.get("title").asText() shouldBe "수정된 제목"
            detail.get("content").asText() shouldBe "수정된 내용"
        }

        test("수정해도 좋아요와 댓글이 보존된다") {
            // Given - 좋아요 1 + 댓글 1
            val email = "patch-preserve@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            insertComment(postId, authorId)
            likePost(postId, authorId)

            // When
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
                content = editRequest(category = "CHEER", title = "보존 확인 제목", content = "보존 확인 내용")
            }.andExpect { status { isOk() } }

            // Then - 좋아요/댓글은 그대로, 제목은 갱신
            val getResult = mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $token")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            val detail = mapper.readTree(getResult.response.contentAsByteArray)
            detail.get("likes").asInt() shouldBe 1
            detail.get("comments").size() shouldBe 1
            detail.get("title").asText() shouldBe "보존 확인 제목"
        }

        test("타인이 수정하면 403을 반환한다") {
            // Given
            val ownerEmail = "patch-owner2@example.com"
            val ownerId = insertMember(ownerEmail)
            val postId = insertPost(ownerId)
            val otherEmail = "patch-other@example.com"
            insertMember(otherEmail)
            val otherToken = login(otherEmail)

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $otherToken")
                content = editRequest()
            }.andExpect { status { isForbidden() } }
        }

        test("존재하지 않는 게시글을 수정하면 404를 반환한다") {
            // Given
            val email = "patch-notfound@example.com"
            insertMember(email)
            val token = login(email)

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", 999L) {
                authorization("Bearer $token")
                content = editRequest()
            }.andExpect { status { isNotFound() } }
        }

        test("이미 삭제된 게시글을 수정하면 404를 반환한다") {
            // Given - 삭제된 게시글
            val email = "patch-deleted@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
                content = editRequest()
            }.andExpect { status { isNotFound() } }
        }

        test("제목이 비어있으면 400을 반환한다") {
            // Given
            val email = "patch-blank-title@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
                content = editRequest(title = "")
            }.andExpect { status { isBadRequest() } }
        }

        test("제목이 최대 글자수를 초과하면 400을 반환한다") {
            // Given
            val email = "patch-long-title@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
                content = editRequest(title = "가".repeat(PostDetails.MAX_TITLE_LENGTH + 1))
            }.andExpect { status { isBadRequest() } }
        }

        test("내용이 비어있으면 400을 반환한다") {
            // Given
            val email = "patch-blank-content@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
                content = editRequest(content = "")
            }.andExpect { status { isBadRequest() } }
        }

        test("내용이 최대 글자수를 초과하면 400을 반환한다") {
            // Given
            val email = "patch-long-content@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
                content = editRequest(content = "가".repeat(PostDetails.MAX_CONTENT_LENGTH + 1))
            }.andExpect { status { isBadRequest() } }
        }

        test("인증 토큰 없이 수정하면 401을 반환한다") {
            // Given
            val postId = insertPost(1L)

            // When & Then
            mockMvc.patchJson("/api/community/posts/{postId}", postId) {
                content = editRequest()
            }.andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/community/posts/{postId}") {

        test("본인이 삭제하면 204를 반환한다") {
            // Given
            val email = "delete-owner@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)

            // When & Then
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }
        }

        test("삭제 후 상세를 조회하면 404를 반환한다") {
            // Given
            val email = "delete-then-detail@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }

            // When & Then
            mockMvc.getJson("/api/community/posts/{id}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        test("삭제하면 목록 조회에서 제외된다") {
            // Given
            val email = "delete-then-list@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }

            // When
            val listResult = mockMvc.getJson("/api/community/posts") {
                authorization("Bearer $token")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val ids = mapper.readTree(listResult.response.contentAsString)
                .get("data")
                .map { it.get("id").asLong() }
            ids shouldNotContain postId
        }

        test("타인이 삭제하면 403을 반환한다") {
            // Given
            val ownerEmail = "delete-owner2@example.com"
            val ownerId = insertMember(ownerEmail)
            val postId = insertPost(ownerId)
            val otherEmail = "delete-other@example.com"
            insertMember(otherEmail)
            val otherToken = login(otherEmail)

            // When & Then
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $otherToken")
            }.andExpect { status { isForbidden() } }
        }

        test("존재하지 않는 게시글을 삭제하면 404를 반환한다") {
            // Given
            val email = "delete-notfound@example.com"
            insertMember(email)
            val token = login(email)

            // When & Then
            mockMvc.deleteJson("/api/community/posts/{postId}", 999L) {
                authorization("Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        test("이미 삭제된 게시글을 다시 삭제하면 404를 반환한다") {
            // Given - 한 번 삭제한 상태
            val email = "delete-twice@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }

            // When & Then - 반복 삭제는 404
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        test("인증 토큰 없이 삭제하면 401을 반환한다") {
            // Given
            val postId = insertPost(1L)

            // When & Then
            mockMvc.deleteJson("/api/community/posts/{postId}", postId)
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("삭제된 게시글 정책") {

        test("삭제된 게시글에 댓글을 작성하면 404를 반환한다") {
            // Given - 삭제된 게시글
            val email = "deleted-comment@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/comments", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("content" to "삭제된 글에 남기는 댓글"))
            }.andExpect { status { isNotFound() } }
        }

        test("삭제된 게시글에 좋아요를 누르면 404를 반환한다") {
            // Given - 삭제된 게시글
            val email = "deleted-like@example.com"
            val authorId = insertMember(email)
            val token = login(email)
            val postId = insertPost(authorId)
            mockMvc.deleteJson("/api/community/posts/{postId}", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/likes", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isNotFound() } }
        }
    }
})
