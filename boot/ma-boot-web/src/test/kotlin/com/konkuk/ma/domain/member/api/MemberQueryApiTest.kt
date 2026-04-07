package com.konkuk.ma.domain.member.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.member.application.MemberQueryService
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.duplicated
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.nickname
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(MemberQueryApi::class)
@BaseApiTest
class MemberQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val memberQueryService: MemberQueryService
) : FunSpec({

    test("닉네임 중복 확인 API 문서화") {
        // Given
        val nickname = "테스트닉네임"

        // When & Then
        every { memberQueryService.checkDuplicatedNickname(nickname) } returns false

        mockMvc.postJson("/api/members/nickname/exists")
        { content = """{"nickname":"$nickname"}""" }
            .andExpect {
                status { { isOk() } }
                jsonPath("$.nickname").value(nickname)
                jsonPath("$.duplicated").value(false) }
            .andDocument(
                "check-nickname-exists",
                requestBody(
                    nickname(),
                ),
                responseBody(
                    nickname(),
                    duplicated(),
                )
            )
    }

    test("유효하지 않은 닉네임 형식으로 요청 시 실패") {
        // Given
        val invalidNickname = "a"

        // When & Then
        mockMvc.postJson("/api/members/nickname/exists")
        { content = """{"nickname":"$invalidNickname"}""" }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "check-nickname-exists-invalid-format",
                requestBody(
                    nickname() means "유효하지 않은 형식의 닉네임",
                )
            )
    }

    test("이메일 중복 확인 API 문서화") {
        // Given
        val email = "test@example.com"

        // When & Then
        every { memberQueryService.checkDuplicatedEmail(email) } returns false

        mockMvc.postJson("/api/members/email/exists")
        { content = """{"email":"$email"}""" }
            .andExpect {
                status { { isOk() } }
                jsonPath("$.email").value(email)
                jsonPath("$.duplicated").value(false) }
            .andDocument(
                "check-email-exists",
                requestBody(
                    email(),
                ),
                responseBody(
                    email(),
                    duplicated(),
                )
            )
    }

    test("유효하지 않은 이메일 형식으로 요청 시 실패") {
        // Given
        val invalidEmail = "invalid-email"

        // When & Then
        mockMvc.postJson("/api/members/email/exists")
        { content = """{"email":"$invalidEmail"}""" }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "check-email-exists-invalid-format",
                requestBody(
                    email() means "유효하지 않은 형식의 이메일",
                )
            )
    }
})
