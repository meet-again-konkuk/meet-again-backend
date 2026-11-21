package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.community.entity.table.ReportTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.ResultRow
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

    fun insertMember(email: String, rawPassword: String? = null, nickname: String = "회원-$email"): Long {
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

    fun insertAuthor(nickname: String? = null): Long {
        val email = nextEmail("author")
        return insertMember(email = email, nickname = nickname ?: "작성자-$email")
    }

    fun insertPost(authorId: Long, title: String = "신고 대상 게시글", content: String = "내용"): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorId] = authorId
                it[category] = "CHEER"
                it[PostTable.title] = title
                it[PostTable.content] = content
            }.value
        }
    }

    fun insertComment(postId: Long, authorId: Long, content: String = "신고 대상 댓글"): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[CommentTable.content] = content
            }.value
        }
    }

    fun findStoredReport(reporterId: Long, targetType: String, targetId: Long): ResultRow? {
        return transaction {
            ReportTable.selectAll()
                .where {
                    (ReportTable.reporterId eq reporterId) and
                        (ReportTable.targetType eq targetType) and
                        (ReportTable.targetId eq targetId) and
                        (ReportTable.deleted eq false)
                }
                .singleOrNull()
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
            val (_, token) = loginReporter()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)

            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isCreated() } }
        }

        test("게시글을 신고하면 대상 작성자·제목·본문 스냅샷과 RECEIVED 상태가 저장된다") {
            val (reporterId, token) = loginReporter()
            val authorId = insertAuthor()
            val postTitle = "혐오 표현이 담긴 제목"
            val postContent = "혐오 표현이 담긴 본문"
            val postId = insertPost(authorId = authorId, title = postTitle, content = postContent)
            val reason = "HATE"

            val result = mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to reason))
            }.andExpect { status { isCreated() } }.andReturn()

            val body = mapper.readTree(result.response.contentAsString)
            body.get("reportId").asLong() shouldBeGreaterThan 0L
            body.get("status").asText() shouldBe "RECEIVED"

            val stored = findStoredReport(reporterId, "POST", postId)
            stored.shouldNotBeNull()
            stored[ReportTable.targetAuthorId] shouldBe authorId
            stored[ReportTable.targetTitle] shouldBe postTitle
            stored[ReportTable.targetContent] shouldBe postContent
            stored[ReportTable.reason] shouldBe reason
            stored[ReportTable.status] shouldBe "RECEIVED"
        }

        test("본인 게시글을 신고하면 400을 반환한다") {
            val (reporterId, token) = loginReporter()
            val postId = insertPost(authorId = reporterId)

            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isBadRequest() } }
        }

        test("이미 신고한 게시글을 다시 신고하면 409를 반환한다") {
            val (reporterId, token) = loginReporter()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)
            insertReport(reporterId, "POST", postId, authorId)

            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isConflict() } }
        }

        test("존재하지 않는 게시글을 신고하면 404를 반환한다") {
            val (_, token) = loginReporter()

            mockMvc.postJson("/api/community/posts/{postId}/reports", 999_999L) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isNotFound() } }
        }

        test("detail이 500자를 초과하면 400을 반환한다") {
            val (_, token) = loginReporter()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)
            val overSizeDetail = "가".repeat(501)

            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM", "detail" to overSizeDetail))
            }.andExpect { status { isBadRequest() } }
        }

        test("인증 토큰 없이 신고하면 401을 반환한다") {
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)

            mockMvc.postJson("/api/community/posts/{postId}/reports", postId) {
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/community/comments/{commentId}/reports") {

        test("타인 댓글을 정상 신고하면 201을 반환한다") {
            val (_, token) = loginReporter()
            val authorId = insertAuthor(nickname = "댓글작성자")
            val postId = insertPost(authorId = authorId)
            val commentId = insertComment(postId = postId, authorId = authorId)

            mockMvc.postJson("/api/community/comments/{commentId}/reports", commentId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "HARASSMENT"))
            }.andExpect { status { isCreated() } }
        }

        test("댓글을 신고하면 대상 작성자·내용 스냅샷이 저장되고 제목 스냅샷은 null이다") {
            val (reporterId, token) = loginReporter()
            val authorId = insertAuthor(nickname = "댓글작성자")
            val postId = insertPost(authorId = authorId)
            val commentContent = "신고 대상 댓글 본문"
            val commentId = insertComment(postId = postId, authorId = authorId, content = commentContent)

            mockMvc.postJson("/api/community/comments/{commentId}/reports", commentId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "HARASSMENT", "detail" to "괴롭힘"))
            }.andExpect { status { isCreated() } }

            val stored = findStoredReport(reporterId, "COMMENT", commentId)
            stored.shouldNotBeNull()
            stored[ReportTable.targetAuthorId] shouldBe authorId
            stored[ReportTable.targetTitle].shouldBeNull()
            stored[ReportTable.targetContent] shouldBe commentContent
        }

        test("본인이 작성한 댓글을 신고하면 400을 반환한다") {
            val (reporterId, token) = loginReporter()
            val postId = insertPost(authorId = reporterId)
            val commentId = insertComment(postId = postId, authorId = reporterId)

            mockMvc.postJson("/api/community/comments/{commentId}/reports", commentId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isBadRequest() } }
        }

        test("이미 신고한 댓글을 다시 신고하면 409를 반환한다") {
            val (reporterId, token) = loginReporter()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)
            val commentId = insertComment(postId = postId, authorId = authorId)
            insertReport(reporterId, "COMMENT", commentId, authorId)

            mockMvc.postJson("/api/community/comments/{commentId}/reports", commentId) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isConflict() } }
        }

        test("존재하지 않는 댓글을 신고하면 404를 반환한다") {
            val (_, token) = loginReporter()

            mockMvc.postJson("/api/community/comments/{commentId}/reports", 999_999L) {
                authorization("Bearer $token")
                content = mapper.writeValueAsString(mapOf("reason" to "SPAM"))
            }.andExpect { status { isNotFound() } }
        }
    }
})
