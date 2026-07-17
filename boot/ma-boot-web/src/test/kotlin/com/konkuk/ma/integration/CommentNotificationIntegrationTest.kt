package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.community.entity.table.BlockTable
import com.konkuk.ma.domain.community.entity.table.CommentNotificationSettingTable
import com.konkuk.ma.domain.community.entity.table.CommentTable
import com.konkuk.ma.domain.community.entity.table.PostTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.domain.notification.entity.table.NotificationTable
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.FunSpec
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.jetbrains.exposed.sql.transactions.transaction

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentNotificationIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    val allTables = arrayOf(
        MemberTable, RefreshTokenTable, PostTable, CommentTable,
        BlockTable, CommentNotificationSettingTable, NotificationTable,
    )

    beforeSpec {
        transaction { SchemaUtils.create(*allTables) }
    }

    afterEach {
        transaction { allTables.reversed().forEach { it.deleteAll() } }
    }

    afterSpec {
        transaction { SchemaUtils.drop(*allTables) }
    }

    fun insertMember(email: String, nickname: String, rawPassword: String = "password123"): Long = transaction {
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

    fun insertPost(authorId: Long): Long = transaction {
        PostTable.insertAndGetId {
            it[PostTable.authorId] = authorId
            it[category] = "CHEER"
            it[title] = "테스트 게시글"
            it[content] = "내용"
        }.value
    }

    fun login(email: String, password: String = "password123"): String {
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }
            .andExpect { status { isOk() } }
            .andReturn()
        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    fun createComment(postId: Long, accessToken: String, content: String = "댓글입니다") =
        mockMvc.postJson("/api/community/posts/$postId/comments") {
            authorization("Bearer $accessToken")
            this.content = mapper.writeValueAsString(mapOf("content" to content))
        }.andExpect { status { isCreated() } }

    fun notificationsOf(recipientId: Long) = transaction {
        NotificationTable.selectAll()
            .where { NotificationTable.recipientId eq recipientId }
            .toList()
    }

    context("댓글 작성 → 알림 생성") {

        test("타인이 내 게시글에 댓글을 달면 글 작성자에게 COMMENT_ON_POST 알림이 생성된다") {
            // Given
            val authorId = insertMember(email = "noti-author@example.com", nickname = "글쓴이")
            val commenterId = insertMember(email = "noti-commenter@example.com", nickname = "댓글러")
            val postId = insertPost(authorId)

            // When
            createComment(postId, login("noti-commenter@example.com"))

            // Then
            val notifications = notificationsOf(authorId)
            notifications.size shouldBe 1
            notifications.first()[NotificationTable.type] shouldBe "COMMENT_ON_POST"
            notifications.first()[NotificationTable.actorId] shouldBe commenterId
            notifications.first()[NotificationTable.postId] shouldBe postId
            notifications.first()[NotificationTable.read] shouldBe false
        }

        test("자기 게시글에 자기가 댓글을 달면 알림이 생성되지 않는다") {
            // Given
            val authorId = insertMember(email = "noti-self@example.com", nickname = "글쓴이")
            val postId = insertPost(authorId)

            // When
            createComment(postId, login("noti-self@example.com"))

            // Then
            notificationsOf(authorId).size shouldBe 0
        }

        test("게시글 알림을 꺼둔(opt-out) 상태면 타인 댓글에도 알림이 생성되지 않는다") {
            // Given
            val authorEmail = "noti-optout-author@example.com"
            val authorId = insertMember(email = authorEmail, nickname = "글쓴이")
            insertMember(email = "noti-optout-commenter@example.com", nickname = "댓글러")
            val postId = insertPost(authorId)

            mockMvc.patchJson("/api/community/posts/$postId/comment-notification") {
                authorization("Bearer ${login(authorEmail)}")
                content = mapper.writeValueAsString(mapOf("enabled" to false))
            }.andExpect { status { isOk() } }

            // When
            createComment(postId, login("noti-optout-commenter@example.com"))

            // Then
            notificationsOf(authorId).size shouldBe 0
        }
    }

    context("알림 조회 → 읽음 처리 흐름") {

        test("글 작성자가 알림 목록을 조회하고 읽음 처리하면 안읽음 카운트가 0이 된다") {
            // Given - 타인 댓글로 알림 1건 생성
            val authorEmail = "noti-flow-author@example.com"
            val authorId = insertMember(email = authorEmail, nickname = "글쓴이")
            insertMember(email = "noti-flow-commenter@example.com", nickname = "댓글러")
            val postId = insertPost(authorId)
            createComment(postId, login("noti-flow-commenter@example.com"))
            val authorToken = login(authorEmail)

            // When - 목록 조회 (actor 닉네임 포함 응답 검증)
            val listResult = mockMvc.getJson("/api/notifications") {
                authorization("Bearer $authorToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            // 무인자 contentAsString은 ISO-8859-1로 디코딩되어 한글이 깨진다
            val body = mapper.readTree(listResult.response.getContentAsString(Charsets.UTF_8))
            body.get("data").size() shouldBe 1
            body.get("data").get(0).get("actorNickname").asText() shouldBe "댓글러"
            body.get("data").get(0).get("read").asBoolean() shouldBe false
            val notificationId = body.get("data").get(0).get("id").asLong()

            // When - 단건 읽음 처리
            mockMvc.patchJson("/api/notifications/$notificationId/read") {
                authorization("Bearer $authorToken")
            }.andExpect { status { isOk() } }

            // Then - 안읽음 카운트 0
            val countResult = mockMvc.getJson("/api/notifications/unread-count") {
                authorization("Bearer $authorToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()
            mapper.readTree(countResult.response.contentAsString).get("count").asLong() shouldBe 0L
        }
    }
})
