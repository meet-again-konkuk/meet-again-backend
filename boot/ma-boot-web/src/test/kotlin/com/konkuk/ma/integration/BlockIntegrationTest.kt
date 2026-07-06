package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * 차단(REQ-014) API E2E 통합 테스트 (REST Docs 제외, HTTP 상태 계약 검증).
 *
 * POST /author/block(최초 201·재차단 200·본인 400)·GET /blocks(200)·
 * DELETE /blocks/{blockId}(204·타인 403·없음 404)·미인증(401) 상태 매핑을 API → Service → DB 관통으로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlockIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    var memberSeq = 0L

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, CommentTable, BlockTable)
        }
    }

    afterEach {
        transaction {
            BlockTable.deleteAll()
            CommentTable.deleteAll()
            PostTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(BlockTable, CommentTable, PostTable, RefreshTokenTable, MemberTable)
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
                it[title] = "게시글"
                it[content] = "내용"
            }.value
        }
    }

    fun insertComment(postId: Long, authorId: Long): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "댓글"
            }.value
        }
    }

    fun insertBlock(blockerId: Long, blockedId: Long): Long {
        return transaction {
            BlockTable.insertAndGetId {
                it[BlockTable.blockerId] = blockerId
                it[BlockTable.blockedId] = blockedId
            }.value
        }
    }

    // 차단자(로그인 회원)를 만들고 로그인해 (memberId, accessToken) 을 반환한다.
    fun loginBlocker(): Pair<Long, String> {
        val rawPassword = "password123"
        val email = nextEmail("blocker")
        val blockerId = insertMember(email = email, rawPassword = rawPassword, nickname = "차단자")
        val request = mapOf("email" to email, "password" to rawPassword)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()
        val token = mapper.readTree(result.response.contentAsString).get("accessToken").asText()
        return blockerId to token
    }

    context("POST /api/community/posts/{postId}/author/block") {

        test("게시글 작성자를 처음 차단하면 201을 반환한다") {
            // Given
            val (_, token) = loginBlocker()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/author/block", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isCreated() } }
        }

        test("이미 차단한 작성자를 다시 차단하면 200을 반환한다(idempotent)") {
            // Given - 기존 활성 차단 존재
            val (blockerId, token) = loginBlocker()
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)
            insertBlock(blockerId = blockerId, blockedId = authorId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/author/block", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isOk() } }
        }

        test("본인이 작성한 게시글의 작성자(=본인)를 차단하면 400을 반환한다") {
            // Given
            val (blockerId, token) = loginBlocker()
            val postId = insertPost(authorId = blockerId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/author/block", postId) {
                authorization("Bearer $token")
            }.andExpect { status { isBadRequest() } }
        }

        test("인증 토큰 없이 차단하면 401을 반환한다") {
            // Given
            val authorId = insertAuthor()
            val postId = insertPost(authorId = authorId)

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/author/block", postId)
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/community/comments/{commentId}/author/block") {

        test("댓글 작성자를 처음 차단하면 201을 반환한다") {
            // Given
            val (blockerId, token) = loginBlocker()
            val authorId = insertAuthor(nickname = "댓글작성자")
            val postId = insertPost(authorId = blockerId)
            val commentId = insertComment(postId = postId, authorId = authorId)

            // When & Then
            mockMvc.postJson("/api/community/comments/{commentId}/author/block", commentId) {
                authorization("Bearer $token")
            }.andExpect { status { isCreated() } }
        }
    }

    context("GET /api/community/blocks") {

        test("내 차단 목록을 조회하면 200을 반환한다") {
            // Given
            val (blockerId, token) = loginBlocker()
            val blockedId = insertAuthor(nickname = "차단대상")
            insertBlock(blockerId = blockerId, blockedId = blockedId)

            // When & Then
            mockMvc.getJson("/api/community/blocks") {
                authorization("Bearer $token")
            }.andExpect { status { isOk() } }
        }

        test("인증 토큰 없이 차단 목록을 조회하면 401을 반환한다") {
            // When & Then
            mockMvc.getJson("/api/community/blocks")
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/community/blocks/{blockId}") {

        test("본인이 만든 차단을 해제하면 204를 반환한다") {
            // Given
            val (blockerId, token) = loginBlocker()
            val blockedId = insertAuthor(nickname = "차단대상")
            val blockId = insertBlock(blockerId = blockerId, blockedId = blockedId)

            // When & Then
            mockMvc.deleteJson("/api/community/blocks/{blockId}", blockId) {
                authorization("Bearer $token")
            }.andExpect { status { isNoContent() } }
        }

        test("타인이 만든 차단을 해제하려 하면 403을 반환한다") {
            // Given - 다른 회원이 만든 차단
            val (_, token) = loginBlocker()
            val ownerId = insertAuthor(nickname = "차단소유자")
            val blockedId = insertAuthor(nickname = "차단대상")
            val blockId = insertBlock(blockerId = ownerId, blockedId = blockedId)

            // When & Then
            mockMvc.deleteJson("/api/community/blocks/{blockId}", blockId) {
                authorization("Bearer $token")
            }.andExpect { status { isForbidden() } }
        }

        test("존재하지 않는 차단을 해제하려 하면 404를 반환한다") {
            // Given
            val (_, token) = loginBlocker()

            // When & Then
            mockMvc.deleteJson("/api/community/blocks/{blockId}", 999_999L) {
                authorization("Bearer $token")
            }.andExpect { status { isNotFound() } }
        }

        test("인증 토큰 없이 차단을 해제하면 401을 반환한다") {
            // When & Then
            mockMvc.deleteJson("/api/community/blocks/{blockId}", 999_999L)
                .andExpect { status { isUnauthorized() } }
        }
    }
})
