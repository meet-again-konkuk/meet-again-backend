package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.community.entity.table.ReportTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
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
 * 신고(REQ-014) API E2E 통합 테스트 (REST Docs 제외, HTTP 상태 계약 검증).
 *
 * POST /api/community/posts/{postId}/reports · /comments/{commentId}/reports 의
 * 성공(201)·본인 신고(400)·중복(409)·없는 대상(404)·detail 초과(400)·미인증(401) 상태 매핑을
 * API → Service → DB 관통으로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    var memberSeq = 0L

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, CommentTable, ReportTable)
        }
    }

    afterEach {
        transaction {
            ReportTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(ReportTable, CommentTable, PostTable, RefreshTokenTable, MemberTable)
        }
    }

    fun nextEmail(prefix: String): String {
        memberSeq += 1
        return "$prefix$memberSeq@example.com"
    }

    fun insertMember(email: String, rawPassword: String? = null, nickname: String = "회원"): Long {
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

    fun insertAuthor(nickname: String = "작성자"): Long {
        return insertMember(email = nextEmail("author"), nickname = nickname)
    }

    fun insertPost(authorId: Long): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[category] = "CHEER"
                it[title] = "신고 대상 게시글"
                it[content] = "내용"
            }.value
        }
    }

    fun insertComment(postId: Long, authorId: Long): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "신고 대상 댓글"
            }.value
        }
    }

    fun insertReport(reporterId: Long, targetType: String, targetId: Long, targetAuthorId: Long) {
        transaction {
            ReportTable.insert {
                it[ReportTable.reporterId] = reporterId
                it[ReportTable.targetType] = targetType
                it[ReportTable.targetId] = targetId
                it[ReportTable.targetAuthorId] = targetAuthorId
                it[targetContent] = "기존 신고 스냅샷"
                it[reason] = "SPAM"
                it[status] = "RECEIVED"
            }
        }
    }

    // 신고자(로그인 회원)를 만들고 로그인해 (memberId, accessToken) 을 반환한다.
    fun loginReporter(): Pair<Long, String> {
        val rawPassword = "password123"
        val email = nextEmail("reporter")
        val reporterId = insertMember(email = email, rawPassword = rawPassword, nickname = "신고자")
        val request = mapOf("email" to email, "password" to rawPassword)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()
        val token = mapper.readTree(result.response.contentAsString).get("accessToken").asText()
        return reporterId to token
    }

    context("POST /api/community/posts/{postId}/reports") {

        test("타인 게시글을 정상 신고하면 201을 반환한다") {
            // Given
            val (_, token) = loginReporter()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isCreated() } }
        }

        test("본인 게시글을 신고하면 400을 반환한다") {
            // Given - 로그인한 회원이 자기 게시글을 신고
            val (reporterId, token) = loginReporter()
            val postId = insertPost(authorId = reporterId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isBadRequest() } }
        }

        test("이미 신고한 게시글을 다시 신고하면 409를 반환한다") {
            // Given - 동일 신고자·대상의 기존 활성 신고 존재
            val (reporterId, token) = loginReporter()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)
            insertReport(reporterId, "POST", postId, authorId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isConflict() } }
        }

        test("존재하지 않는 게시글을 신고하면 404를 반환한다") {
            // Given
            val (_, token) = loginReporter()

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/reports", 999_999L) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isNotFound() } }
        }

        test("detail이 500자를 초과하면 400을 반환한다") {
            // Given
            val (_, token) = loginReporter()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)
            val overSizeDetail = "가".repeat(501)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM", "detail" to overSizeDetail))
            }.andExpect { status { isBadRequest() } }
        }

        test("인증 토큰 없이 신고하면 401을 반환한다") {
            // Given
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/community/comments/{commentId}/reports") {

        test("타인 댓글을 정상 신고하면 201을 반환한다") {
            // Given
            val (_, token) = loginReporter()
            val authorId = insertAuthor(nickname = "댓글작성자")
            val postId = insertPost(authorId = authorId)
            val commentId = insertComment(postId = postId, authorId = authorId)

            // When & Then
            mockMvc.postJson("/api/community/comments/{commentId}/reports", commentId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "HARASSMENT"))
            }.andExpect { status { isCreated() } }
        }

        test("존재하지 않는 댓글을 신고하면 404를 반환한다") {
            // Given
            val (_, token) = loginReporter()

            // When & Then
            mockMvc.postJson("/api/community/comments/{commentId}/reports", 999_999L) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isNotFound() } }
        }
    }
})
