package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.domain.ClaimStatus
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * 매칭 claim 거절(PATCH /api/matching-results/{id}/reject) API E2E 통합 테스트.
 *
 * API(MatchingResultCommandApi) → MatchingResultCommandService.reject → MatchingResult(도메인 전이) →
 * MatchingResultRepository.updateClaimStatus → MATCHING_RESULTS 테이블을 실제로 관통하며 검증한다.
 *
 * - 거절 권한은 수신자(targetId)만 가진다. 인증은 실제 로그인 흐름(JWT)으로 수행하고, 인증 회원 id 를
 *   매칭의 targetId 로 맞춰 소유권 검증을 통과시킨다.
 * - matchingResultId 는 @DecryptId 대상이라 요청 경로에 난독화 id 를 인코딩해 전달한다.
 * - DB 상태 검증은 transaction{} 으로 MATCHING_RESULTS 를 직접 읽어 CLAIM_STATUS 를 확인한다(포트/서비스 경유 금지).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchingClaimRejectIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, MatchingResultTable)
        }
    }

    afterEach {
        transaction {
            MatchingResultTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(MatchingResultTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(
        email: String,
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
        targetId: Long,
        claimStatus: ClaimStatus,
        targetInfoId: Long = 1L,
    ): Long {
        return transaction {
            MatchingResultTable.insertAndGetId {
                it[MatchingResultTable.registerId] = registerId
                it[MatchingResultTable.targetInfoId] = targetInfoId
                it[MatchingResultTable.targetId] = targetId
                it[middleNumberMatched] = false
                it[lastNumberMatched] = false
                it[yearMatched] = false
                it[monthMatched] = false
                it[dayMatched] = false
                it[regionMatched] = false
                it[showingExpiryDate] = LocalDateTime.now().plusDays(7)
                it[matchingExpiryDate] = LocalDate.now().plusDays(30)
                it[excluded] = false
                it[MatchingResultTable.claimStatus] = claimStatus
            }.value
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

    fun reject(encodedId: String, accessToken: String) =
        mockMvc.patchJson("/api/matching-results/$encodedId/reject") {
            authorization("Bearer $accessToken")
        }

    fun claimStatusOf(matchingResultId: Long): ClaimStatus = transaction {
        MatchingResultTable.selectAll()
            .where { MatchingResultTable.id eq matchingResultId }
            .first()[MatchingResultTable.claimStatus]
    }

    context("PATCH /api/matching-results/{matchingResultId}/reject") {

        test("수신자가 CLAIMED 매칭을 거절하면 200을 반환하고 DB의 CLAIM_STATUS가 REJECTED로 변경된다") {
            // Given - 수신자(target=로그인 회원)에게 온 CLAIMED 매칭
            val recipientEmail = "matching-reject-recipient@example.com"
            val recipientPassword = "password123"
            val ownerId = insertMember(email = "matching-reject-owner@example.com")
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val matchingResultId = insertMatchingResult(
                registerId = ownerId,
                targetId = recipientId,
                claimStatus = ClaimStatus.CLAIMED,
            )
            val accessToken = login(recipientEmail, recipientPassword)
            val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

            // When
            reject(encodedId, accessToken)
                .andExpect { status { isOk() } }

            // Then - DB의 실제 상태가 REJECTED로 바뀐다
            claimStatusOf(matchingResultId) shouldBe ClaimStatus.REJECTED
        }

        test("CLAIMED 상태가 아니면(NONE) 400을 반환하고 DB 상태가 변하지 않는다") {
            // Given - 아직 claim되지 않은(NONE) 매칭
            val recipientEmail = "matching-reject-none-recipient@example.com"
            val recipientPassword = "password123"
            val ownerId = insertMember(email = "matching-reject-none-owner@example.com")
            val recipientId = insertMember(email = recipientEmail, rawPassword = recipientPassword)
            val matchingResultId = insertMatchingResult(
                registerId = ownerId,
                targetId = recipientId,
                claimStatus = ClaimStatus.NONE,
            )
            val accessToken = login(recipientEmail, recipientPassword)
            val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

            // When & Then - 상태 전이 위반 → 400
            reject(encodedId, accessToken)
                .andExpect { status { isBadRequest() } }

            // Then - DB 상태는 NONE 그대로 유지된다
            claimStatusOf(matchingResultId) shouldBe ClaimStatus.NONE
        }
    }
})
