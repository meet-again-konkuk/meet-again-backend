package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostLikeTable
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

    // 조회 엔드포인트도 인증이 필요하므로, 조회용 로그인 회원을 만들고 토큰을 반환한다.
    fun loginAsViewer(): String {
        insertMember(email = viewerEmail, nickname = "조회자", rawPassword = viewerPassword)
        return login(viewerEmail, viewerPassword)
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

    fun insertComment(postId: Long, authorId: Long): Long {
        return transaction {
            CommentTable.insertAndGetId {
                it[CommentTable.postId] = postId
                it[CommentTable.authorId] = authorId
                it[content] = "테스트 댓글"
            }.value
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

    fun findPostNode(posts: JsonNode, postId: Long): JsonNode {
        return posts.first { it.get("id").asLong() == postId }
    }

    context("GET /api/community/posts (목록)") {

        test("각 게시글의 likes는 실제 좋아요 행 수와 일치한다 - 0개와 N개가 섞인 경우") {
            // Given - 조회자 로그인 + 작성자 회원 + 게시글 3개, 좋아요 수는 각각 0, 2, 5
            val accessToken = loginAsViewer()
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
            val accessToken = loginAsViewer()
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
            val accessToken = loginAsViewer()
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
            val accessToken = loginAsViewer()
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
            val accessToken = loginAsViewer()
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
})
