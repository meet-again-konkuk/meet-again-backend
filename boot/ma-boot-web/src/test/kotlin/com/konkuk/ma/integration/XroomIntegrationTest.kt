package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XroomIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, TargetInfoTable, XroomTable, MatchingResultTable)
        }
    }

    afterEach {
        transaction {
            XroomTable.deleteAll()
            MatchingResultTable.deleteAll()
            TargetInfoTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(MatchingResultTable, XroomTable, TargetInfoTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(
        email: String = "xroom-test@example.com",
        rawPassword: String = "password123",
        name: String = "김테스트",
    ): Long {
        return transaction {
            MemberTable.insertAndGetId {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[nickname] = "테스터"
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[MemberTable.name] = name
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertMatchingResult(
        registerId: Long,
        targetInfoId: Long,
        targetId: Long,
        claimed: Boolean = true,
    ) {
        transaction {
            MatchingResultTable.insert {
                it[MatchingResultTable.registerId] = registerId
                it[MatchingResultTable.targetInfoId] = targetInfoId
                it[MatchingResultTable.targetId] = targetId
                it[middleNumberMatched] = false
                it[lastNumberMatched] = false
                it[yearMatched] = false
                it[monthMatched] = false
                it[dayMatched] = false
                it[regionMatched] = false
                // isVisible()=true 보장: now > showingExpiryDate - 30일
                it[showingExpiryDate] = LocalDateTime.now().plusDays(7)
                it[matchingExpiryDate] = LocalDate.now().plusDays(30)
                it[excluded] = false
                it[MatchingResultTable.claimed] = claimed
            }
        }
    }

    fun insertTargetInfo(
        registerId: Long,
        targetName: String = "홍길동",
    ): Long {
        return transaction {
            TargetInfoTable.insertAndGetId {
                it[TargetInfoTable.registerId] = registerId
                it[name] = targetName
                it[targetGender] = "FEMALE"
                it[region] = "SEOUL"
            }.value
        }
    }

    fun insertXroom(
        ownerId: Long,
        targetInfoId: Long,
        title: String = "기억의 방",
        finalMessage: String? = "고마웠어",
    ): Long {
        return transaction {
            XroomTable.insert {
                it[XroomTable.ownerId] = ownerId
                it[XroomTable.targetInfoId] = targetInfoId
                it[template] = "chat_memory"
                it[XroomTable.title] = title
                it[XroomTable.finalMessage] = finalMessage
            }
            XroomTable
                .activeRows { XroomTable.ownerId eq ownerId }
                .first()[XroomTable.id].value
        }
    }

    fun login(email: String, password: String): String {
        val request = mapOf("email" to email, "password" to password)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()

        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    context("GET /api/xrooms/me") {

        test("로그인 후 내가 만든 방 목록을 조회하면 수신자 이름과 함께 반환한다") {
            // Given
            val email = "xroom-me@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetName = "홍길동"
            val targetInfoId = insertTargetInfo(registerId = memberId, targetName = targetName)
            insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            // When
            val result = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 1
            val room = rooms.get(0)
            room.get("recipientName").asText() shouldBe targetName
            room.get("memoryCount").asInt() shouldBe 0
            room.get("id").asText().isNotBlank() shouldBe true
            room.get("targetInfoId").asText().isNotBlank() shouldBe true
        }

        test("만든 방이 없으면 빈 목록을 반환한다") {
            // Given
            val email = "xroom-empty@example.com"
            val password = "password123"
            insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            // When
            val result = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 0
        }

        test("인증 토큰 없이 내 방 목록을 조회하면 401이 반환된다") {
            // When & Then
            mockMvc.getJson("/api/xrooms/me")
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("POST /api/xrooms") {

        test("로그인 후 본인 TargetInfo로 방을 생성하면 성공하고 내 방 목록에 노출된다") {
            // Given
            val email = "xroom-create@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetName = "김철수"
            val targetInfoId = insertTargetInfo(registerId = memberId, targetName = targetName)
            val accessToken = login(email, password)

            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId),
                "finalMessage" to "보고 싶었어",
            )

            // When
            val createResult = mockMvc.postJson("/api/xrooms") {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            // Then - 응답에 암호화된 xroomId가 담긴다
            val createBody = mapper.readTree(createResult.response.contentAsString)
            createBody.get("xroomId").asText().isNotBlank() shouldBe true

            // Then - 생성한 방이 내 방 목록에 노출된다
            val listResult = mockMvc.getJson("/api/xrooms/me") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            val rooms = mapper.readTree(listResult.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 1
            rooms.get(0).get("recipientName").asText() shouldBe targetName
        }

        test("인증 토큰 없이 방을 생성하면 401이 반환된다") {
            // Given
            val request = mapOf(
                "targetInfoId" to idObfuscator.encode(ObfuscationType.TARGET_INFO, 1L),
                "finalMessage" to "보고 싶었어",
            )

            // When & Then
            mockMvc.postJson("/api/xrooms") {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("PATCH /api/xrooms/{xroomId}") {

        test("작성자가 로그인 후 마지막 메시지를 수정하면 성공한다") {
            // Given
            val email = "xroom-patch@example.com"
            val password = "password123"
            val memberId = insertMember(email = email, rawPassword = password)
            val targetInfoId = insertTargetInfo(registerId = memberId)
            val xroomId = insertXroom(ownerId = memberId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)
            val request = mapOf("finalMessage" to "마지막 인사를 전해")

            // When
            val result = mockMvc.patchJson("/api/xrooms/{xroomId}", encodedXroomId) {
                content = mapper.writeValueAsString(request)
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val body = mapper.readTree(result.response.contentAsString)
            body.get("xroomId").asText() shouldBe encodedXroomId
        }

        test("인증 토큰 없이 마지막 메시지를 수정하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)
            val request = mapOf("finalMessage" to "마지막 인사를 전해")

            // When & Then
            mockMvc.patchJson("/api/xrooms/{xroomId}", encodedXroomId) {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("GET /api/xrooms/received") {

        test("수신자가 로그인하면 자신에게 도착한 방 목록을 보낸 사람 이름과 함께 반환한다") {
            // Given
            val ownerName = "김작성"
            val ownerId = insertMember(email = "xroom-owner@example.com", name = ownerName)
            val recipientPassword = "password123"
            val recipientEmail = "xroom-recipient@example.com"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimed = true,
            )
            insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(recipientEmail, recipientPassword)

            // When
            val result = mockMvc.getJson("/api/xrooms/received") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 1
            val room = rooms.get(0)
            room.get("senderName").asText() shouldBe ownerName
            room.get("memoryCount").asInt() shouldBe 0
            room.get("id").asText().isNotBlank() shouldBe true
        }

        test("수신한 방이 없으면 빈 목록을 반환한다") {
            // Given
            val email = "xroom-no-received@example.com"
            val password = "password123"
            insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            // When
            val result = mockMvc.getJson("/api/xrooms/received") {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val rooms = mapper.readTree(result.response.contentAsByteArray).get("rooms")
            rooms.size() shouldBe 0
        }

        test("인증 토큰 없이 수신한 방 목록을 조회하면 401이 반환된다") {
            // When & Then
            mockMvc.getJson("/api/xrooms/received")
                .andExpect { status { isUnauthorized() } }
        }
    }

    context("GET /api/xrooms/{xroomId}") {

        test("작성자가 자신의 방을 조회하면 상세 정보를 반환한다") {
            // Given
            val email = "xroom-detail-owner@example.com"
            val password = "password123"
            val ownerId = insertMember(email = email, rawPassword = password)
            val targetName = "홍길동"
            val targetInfoId = insertTargetInfo(registerId = ownerId, targetName = targetName)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(email, password)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // When
            val result = mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
                .andReturn()

            // Then
            val body = mapper.readTree(result.response.contentAsByteArray)
            body.get("id").asText() shouldBe encodedXroomId
            body.get("recipientName").asText() shouldBe targetName
            body.get("template").asText() shouldBe "chat_memory"
            body.get("finalMessage").asText().isNotBlank() shouldBe true
            body.get("memories").size() shouldBe 0
        }

        test("수신자가 자신에게 도착한 방을 조회하면 상세 정보를 반환한다") {
            // Given
            val ownerId = insertMember(email = "xroom-detail-sender@example.com")
            val recipientEmail = "xroom-detail-recipient@example.com"
            val recipientPassword = "password123"
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            insertMatchingResult(
                registerId = ownerId,
                targetInfoId = targetInfoId,
                targetId = recipientId,
                claimed = true,
            )
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(recipientEmail, recipientPassword)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // When & Then
            mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isOk() } }
        }

        test("작성자도 수신자도 아니면 403이 반환된다") {
            // Given
            val ownerId = insertMember(email = "xroom-detail-other-owner@example.com")
            val strangerEmail = "xroom-detail-stranger@example.com"
            val strangerPassword = "password123"
            insertMember(email = strangerEmail, rawPassword = strangerPassword)
            val targetInfoId = insertTargetInfo(registerId = ownerId)
            val xroomId = insertXroom(ownerId = ownerId, targetInfoId = targetInfoId)
            val accessToken = login(strangerEmail, strangerPassword)

            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, xroomId)

            // When & Then
            mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId) {
                authorization("Bearer $accessToken")
            }
                .andExpect { status { isForbidden() } }
        }

        test("인증 토큰 없이 방 상세를 조회하면 401이 반환된다") {
            // Given
            val encodedXroomId = idObfuscator.encode(ObfuscationType.XROOM, 1L)

            // When & Then
            mockMvc.getJson("/api/xrooms/{xroomId}", encodedXroomId)
                .andExpect { status { isUnauthorized() } }
        }
    }
})
