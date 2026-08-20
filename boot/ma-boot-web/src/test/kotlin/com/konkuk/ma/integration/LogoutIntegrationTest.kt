package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.extension.postJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
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
 * 로그아웃(POST /api/auth/logout) API E2E 통합 테스트 (REST Docs 제외, HTTP 상태 계약 검증).
 *
 * 실제 로그인으로 accessToken/refreshToken 을 발급받고, API → Service → RefreshTokenTable 을
 * 관통하며 다음 계약을 검증한다.
 *  - 로그아웃 성공(204) + refresh token 행 삭제
 *  - 로그아웃 후 그 refresh token 재발급 시도 → 404 (삭제된 행에서 findOne 이 EntityNotFound → 404)
 *  - 미인증(401)
 *  - 멱등성(재로그아웃 204 — deleteWhere 가 0행에도 예외 없음)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    var memberSeq = 0L

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable)
        }
    }

    afterEach {
        transaction {
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(RefreshTokenTable, MemberTable)
        }
    }

    fun nextEmail(): String {
        memberSeq += 1
        return "logout-test$memberSeq@example.com"
    }

    fun insertMember(email: String, rawPassword: String, nickname: String = "로그아웃유저-$email"): Long {
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

    fun countRefreshTokens(memberId: Long): Long {
        return transaction {
            RefreshTokenTable.selectAll()
                .where { RefreshTokenTable.memberId eq memberId }
                .count()
        }
    }

    // 회원을 만들고 실제 로그인해 (memberId, accessToken, refreshToken) 을 발급받는다.
    fun login(): LoggedInMember {
        val email = nextEmail()
        val rawPassword = "password123"
        val memberId = insertMember(email = email, rawPassword = rawPassword)
        val request = mapOf("email" to email, "password" to rawPassword)
        val result = mockMvc.postJson("/api/auth/login") {
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isOk() } }.andReturn()

        val body = mapper.readTree(result.response.contentAsByteArray)
        return LoggedInMember(
            memberId = memberId,
            accessToken = body.get("accessToken").asText(),
            refreshToken = body.get("refreshToken").asText(),
        )
    }

    context("POST /api/auth/logout") {

        test("인증된 회원이 로그아웃하면 204를 반환하고 해당 회원의 refresh token 행이 삭제된다") {
            // Given - 로그인으로 accessToken 발급 + refresh token 행이 저장되어 있음
            val member = login()
            countRefreshTokens(member.memberId) shouldBe 1L

            // When - 로그아웃
            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer ${member.accessToken}")
            }.andExpect { status { isNoContent() } }

            // Then - 해당 회원의 refresh token 행이 삭제됨
            countRefreshTokens(member.memberId) shouldBe 0L
        }

        test("로그아웃 후 발급받았던 refresh token으로 재발급을 시도하면 404를 반환한다") {
            // Given - 로그인 후 로그아웃 (refresh token 행 삭제됨)
            val member = login()
            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer ${member.accessToken}")
            }.andExpect { status { isNoContent() } }

            // When & Then - 서명은 유효하나 행이 없어 findOne 이 EntityNotFound → 404 (401 아님)
            val request = mapOf("refreshToken" to member.refreshToken)
            mockMvc.postJson("/api/auth/refresh-token") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isNotFound() } }
        }

        test("인증 토큰 없이 로그아웃하면 401을 반환한다") {
            // When & Then - 인증 필터가 컨트롤러 도달 전 차단
            mockMvc.postJson("/api/auth/logout")
                .andExpect { status { isUnauthorized() } }
        }

        test("변조된 Bearer 토큰으로 로그아웃하면 400을 반환한다") {
            // Given - 유효한 accessToken 을 발급받되 서명 부분을 변조 (서명 검증 실패)
            val member = login()
            val tamperedToken = "${member.accessToken}tampered"

            // When & Then - JwtAuthenticationFilter 는 malformed/invalid 토큰을 400 으로 매핑한다
            // (미인증 401 과 구분되는 현행 계약: 만료만 401, 변조/손상 토큰은 400)
            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer $tamperedToken")
            }.andExpect { status { isBadRequest() } }
        }

        test("로그아웃 후 같은 accessToken으로 다시 로그아웃해도 멱등하게 204를 반환한다") {
            // Given - 이미 로그아웃 완료 (refresh token 행 없음)
            val member = login()
            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer ${member.accessToken}")
            }.andExpect { status { isNoContent() } }
            countRefreshTokens(member.memberId) shouldBe 0L

            // When & Then - 삭제가 no-op 이므로 재로그아웃도 204
            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer ${member.accessToken}")
            }.andExpect { status { isNoContent() } }
        }
    }
})

private data class LoggedInMember(
    val memberId: Long,
    val accessToken: String,
    val refreshToken: String,
)
