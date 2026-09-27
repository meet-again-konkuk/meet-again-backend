package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import com.konkuk.ma.extension.postJson
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

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

    fun insertMember(
        email: String = "test@example.com",
        rawPassword: String = "password123",
        nickname: String = "테스터-$email"
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

    context("POST /api/auth/login") {

        test("올바른 이메일과 비밀번호로 로그인하면 JWT 토큰이 반환된다") {
            // Given
            val email = "login-test@example.com"
            val password = "password123"
            insertMember(email = email, rawPassword = password, nickname = "로그인유저")

            val request = mapOf("email" to email, "password" to password)

            // When & Then
            val result = mockMvc.postJson("/api/auth/login") {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isOk() } }
                .andReturn()

            result.response.characterEncoding = "UTF-8"
            val response = mapper.readTree(result.response.contentAsString)
            response.get("email").asText() shouldBe email
            response.get("nickname").asText() shouldBe "로그인유저"
            response.get("accessToken").asText() shouldNotBe null
            response.get("refreshToken").asText() shouldNotBe null
        }

        test("잘못된 비밀번호로 로그인하면 실패한다") {
            // Given
            val email = "wrong-pw-test@example.com"
            insertMember(email = email, rawPassword = "password123")

            val request = mapOf("email" to email, "password" to "wrongPassword1")

            // When & Then
            mockMvc.postJson("/api/auth/login") {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }

        test("존재하지 않는 이메일로 로그인하면 실패한다") {
            // Given
            val request = mapOf(
                "email" to "nonexistent@example.com",
                "password" to "password123"
            )

            // When & Then
            mockMvc.postJson("/api/auth/login") {
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isNotFound() } }
        }
    }
})
