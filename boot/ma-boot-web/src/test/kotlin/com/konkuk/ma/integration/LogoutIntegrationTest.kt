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
            val member = login()
            countRefreshTokens(member.memberId) shouldBe 1L

            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer ${member.accessToken}")
            }.andExpect { status { isNoContent() } }

            countRefreshTokens(member.memberId) shouldBe 0L
        }

        test("로그아웃 후 발급받았던 refresh token으로 재발급을 시도하면 404를 반환한다") {
            val member = login()
            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer ${member.accessToken}")
            }.andExpect { status { isNoContent() } }

            val request = mapOf("refreshToken" to member.refreshToken)
            mockMvc.postJson("/api/auth/refresh-token") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isNotFound() } }
        }

        test("인증 토큰 없이 로그아웃하면 401을 반환한다") {
            mockMvc.postJson("/api/auth/logout")
                .andExpect { status { isUnauthorized() } }
        }

        test("변조된 Bearer 토큰으로 로그아웃하면 400을 반환한다") {
            val member = login()
            val tamperedToken = "${member.accessToken}tampered"

            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer $tamperedToken")
            }.andExpect { status { isBadRequest() } }
        }

        test("로그아웃 후 같은 accessToken으로 다시 로그아웃해도 멱등하게 204를 반환한다") {
            val member = login()
            mockMvc.postJson("/api/auth/logout") {
                authorization("Bearer ${member.accessToken}")
            }.andExpect { status { isNoContent() } }
            countRefreshTokens(member.memberId) shouldBe 0L

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
