package com.konkuk.ma.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TargetInfoIntegrationTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper
) : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()

    beforeSpec {
        transaction {
            SchemaUtils.create(MemberTable, RefreshTokenTable, TargetInfoTable)
        }
    }

    afterEach {
        transaction {
            TargetInfoTable.deleteAll()
            RefreshTokenTable.deleteAll()
            MemberTable.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(TargetInfoTable, RefreshTokenTable, MemberTable)
        }
    }

    fun insertMember(
        email: String = "target-info-test@example.com",
        rawPassword: String = "password123"
    ) {
        transaction {
            MemberTable.insert {
                it[MemberTable.email] = email
                it[password] = passwordEncoder.encode(rawPassword)
                it[nickname] = "테스터"
                it[gender] = "MALE"
                it[phoneNumber] = "01012345678"
                it[name] = "김테스트"
                it[birthDate] = LocalDate.of(1990, 1, 1)
                it[region] = "SEOUL"
            }
        }
    }

    fun login(email: String, password: String): String {
        val request = mapOf("email" to email, "password" to password)
        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andReturn()

        return mapper.readTree(result.response.contentAsString).get("accessToken").asText()
    }

    context("POST /api/target-infos") {

        test("로그인 후 찾는 사람 정보를 등록하면 성공한다") {
            // Given
            val email = "targetinfo-e2e@example.com"
            val password = "password123"
            insertMember(email = email, rawPassword = password)
            val accessToken = login(email, password)

            val request = mapOf(
                "name" to "홍길동",
                "middleNumber" to "1234",
                "lastNumber" to "5678",
                "year" to 1999,
                "month" to 12,
                "day" to 31,
                "region" to "SEOUL"
            )

            // When & Then
            val result = mockMvc.post("/api/target-infos") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
                header("Authorization", "Bearer $accessToken")
            }
                .andExpect { status { isCreated() } }
                .andReturn()

            val response = mapper.readTree(result.response.contentAsString)
            response.get("targetInfoId").asText().isNotBlank() shouldBe true
            response.get("registerEmail").asText() shouldBe email
        }

        test("인증 토큰 없이 찾는 사람 정보를 등록하면 401이 반환된다") {
            // Given
            val request = mapOf(
                "name" to "홍길동",
                "region" to "SEOUL"
            )

            // When & Then
            mockMvc.post("/api/target-infos") {
                contentType = MediaType.APPLICATION_JSON
                content = mapper.writeValueAsString(request)
            }
                .andExpect { status { isUnauthorized() } }
        }
    }
})
