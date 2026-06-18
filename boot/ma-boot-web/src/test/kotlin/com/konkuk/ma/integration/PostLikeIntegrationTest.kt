package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
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
 * 게시글 좋아요 E2E 통합 테스트.
 *
 * 비정규화 카운터 컬럼을 제거하고 좋아요 행(PostLikeTable)에서 COUNT로 도출하도록 리팩터한 동작을
 * API(POST/DELETE /api/community/posts/{postId}/likes) → Service → DB까지 관통하여 검증한다.
 *
 * 핵심 검증: 여러 명이 좋아요/취소를 반복해도 응답 likeCount가 항상 실제 좋아요 행 수와 일치한다
 * (과거 카운터 drift 버그가 재발하지 않는지 확인). 특히 눌렀다 취소하면 0으로 정확히 복귀한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostLikeIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, PostTable, PostLikeTable)
        }
    }

    afterEach {
        transaction {
            PostLikeTable.deleteAll()
            PostTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(PostLikeTable, PostTable, RefreshTokenTable, MemberTable)
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

    fun insertPost(authorEmail: String = "author@example.com"): Long {
        return transaction {
            PostTable.insertAndGetId {
                it[PostTable.authorEmail] = authorEmail
                it[category] = "CHEER"
                it[title] = "테스트 게시글"
                it[content] = "내용"
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

    fun countActiveLikes(postId: Long): Long {
        return transaction {
            PostLikeTable
                .selectAll()
                .where { (PostLikeTable.postId eq postId) and (PostLikeTable.deleted eq false) }
                .count()
        }
    }

    context("POST /api/community/posts/{postId}/likes") {

        test("로그인 후 좋아요를 누르면 likeCount=1을 반환하고 좋아요 행이 1개 생성된다") {
            // Given
            val email = "post-like-e2e@example.com"
            insertMember(email = email)
            val accessToken = login(email)
            val postId = insertPost()

            // When
            val result = mockMvc.postJson("/api/community/posts/{postId}/likes", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 응답 likeCount와 실제 좋아요 행 수가 모두 1
            val response = mapper.readTree(result.response.contentAsString)
            response.get("liked").asBoolean() shouldBe true
            response.get("likeCount").asInt() shouldBe 1
            countActiveLikes(postId) shouldBe 1L
        }

        test("서로 다른 회원 3명이 좋아요를 누르면 likeCount=3이고 좋아요 행도 3개다") {
            // Given - 같은 게시글에 서로 다른 회원 3명이 좋아요
            val postId = insertPost()
            val emails = listOf("liker1@example.com", "liker2@example.com", "liker3@example.com")

            // When - 각 회원이 순차적으로 좋아요
            emails.forEachIndexed { index, email ->
                insertMember(email = email)
                val accessToken = login(email)

                val result = mockMvc.postJson("/api/community/posts/{postId}/likes", postId) {
                    authorization("Bearer $accessToken")
                }
                    .andExpect { status { isCreated() } }
                    .andReturn()

                // Then - 좋아요 누적 수가 행 수와 항상 일치 (카운터 drift 없음)
                val likeCount = mapper.readTree(result.response.contentAsString).get("likeCount").asInt()
                likeCount shouldBe (index + 1)
            }

            // Then - 최종적으로 likeCount=3, 좋아요 행도 3개
            countActiveLikes(postId) shouldBe emails.size.toLong()
        }

        test("인증 토큰 없이 좋아요를 누르면 401이 반환된다") {
            // Given
            val postId = insertPost()

            // When & Then
            mockMvc.postJson("/api/community/posts/{postId}/likes", postId)
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("DELETE /api/community/posts/{postId}/likes") {

        test("좋아요를 취소하면 likeCount가 감소하고 좋아요 행이 삭제된다") {
            // Given - 좋아요 누른 상태
            val email = "post-unlike-e2e@example.com"
            insertMember(email = email)
            val accessToken = login(email)
            val postId = insertPost()
            mockMvc.postJson("/api/community/posts/{postId}/likes", postId) {
                authorization("Bearer $accessToken")
            }.andExpect { status { isCreated() } }

            // When - 취소
            val result = mockMvc.deleteJson("/api/community/posts/{postId}/likes", postId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - likeCount=0으로 정확히 복귀하고 활성 좋아요 행도 0개
            val response = mapper.readTree(result.response.contentAsString)
            response.get("liked").asBoolean() shouldBe false
            response.get("likeCount").asInt() shouldBe 0
            countActiveLikes(postId) shouldBe 0L
        }

        test("3명 좋아요 후 1명이 취소하면 likeCount=2로 행 수와 정확히 일치한다") {
            // Given - 3명이 좋아요
            val postId = insertPost()
            val emails = listOf("multi1@example.com", "multi2@example.com", "multi3@example.com")
            emails.forEach { email ->
                insertMember(email = email)
                val token = login(email)
                mockMvc.postJson("/api/community/posts/{postId}/likes", postId) {
                    authorization("Bearer $token")
                }.andExpect { status { isCreated() } }
            }

            // When - 1명만 취소
            val cancelToken = login(emails.first())
            val result = mockMvc.deleteJson("/api/community/posts/{postId}/likes", postId) {
                authorization("Bearer $cancelToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then - 응답 likeCount=2, 실제 활성 좋아요 행도 2개 (drift 없음)
            mapper.readTree(result.response.contentAsString).get("likeCount").asInt() shouldBe 2
            countActiveLikes(postId) shouldBe 2L
        }

        test("인증 토큰 없이 좋아요를 취소하면 401이 반환된다") {
            // Given
            val postId = insertPost()

            // When & Then
            mockMvc.deleteJson("/api/community/posts/{postId}/likes", postId)
                .andExpect { status { isUnauthorized() } }
        }
    }
})
