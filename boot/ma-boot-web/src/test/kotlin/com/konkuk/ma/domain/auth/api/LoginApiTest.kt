package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.application.LoginService
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.api.request.LoginRequest
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.accessToken
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.loginStatus
import com.konkuk.ma.vocabulary.nickname
import com.konkuk.ma.vocabulary.password
import com.konkuk.ma.vocabulary.refreshToken
import com.konkuk.ma.vocabulary.withdrawalExpiresAt
import com.konkuk.ma.vocabulary.withdrawalRequestedAt
import com.konkuk.ma.domain.common.domain.Email
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import java.time.LocalDateTime
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(LoginApi::class)
@BaseApiTest
class LoginApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val loginService: LoginService
) : FunSpec({

    test("로그인 API 문서화") {
        val request = LoginRequest(email = "user@example.com", password = "password1")
        val loginInfo = LoginInfo.Active(
            email = Email(request.email),
            nickname = "tester",
            accessToken = "access-token",
            refreshToken = RefreshToken(Email("user@example.com"), LocalDateTime.now().plusDays(7), "refresh-token")
        )
        every { loginService.login(match { it.email == Email(request.email) && it.password == request.password }) } returns loginInfo

        mockMvc.postJson("/api/auth/login") { content = mapper.writeValueAsString(request) }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.email").value("user@example.com") }
            .andExpect { jsonPath("$.accessToken").value("access-token") }
            .andDocument(
                "auth-login",
                requestBody(
                    email(),
                    password(),
                ),
                responseBody(
                    loginStatus(),
                    email(),
                    nickname(),
                    accessToken(),
                    refreshToken(),
                    withdrawalRequestedAt(),
                    withdrawalExpiresAt(),
                )
            )
    }

    test("로그인 - 탈퇴 신청 중인 회원이면 WITHDRAWAL_PENDING 상태로 응답한다") {
        val request = LoginRequest(email = "pending@example.com", password = "password1")
        val loginInfo = LoginInfo.WithdrawalPending(
            email = Email(request.email),
            accessToken = "access-token",
            refreshToken = RefreshToken(Email(request.email), LocalDateTime.now().plusDays(7), "refresh-token"),
            withdrawalRequestedAt = LocalDateTime.of(2026, 5, 1, 10, 0),
            withdrawalExpiresAt = LocalDateTime.of(2026, 5, 8, 10, 0)
        )
        every { loginService.login(match { it.email == Email(request.email) && it.password == request.password }) } returns loginInfo

        mockMvc.postJson("/api/auth/login") { content = mapper.writeValueAsString(request) }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.status").value("WITHDRAWAL_PENDING") }
    }

    test("로그인 - 유효하지 않은 이메일 형식으로 요청 시 실패한다") {
        val badRequest = mapOf(
            "email" to "invalid-email",
            "password" to "password1"
        )

        mockMvc.postJson("/api/auth/login") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "auth-login-invalid-email",
                requestBody(
                    email() means "잘못된 이메일 형식",
                    password(),
                )
            )
    }

    test("로그인 - 비밀번호가 비어있으면 400을 반환한다") {
        val badRequest = mapOf(
            "email" to "user@example.com",
            "password" to ""
        )

        mockMvc.postJson("/api/auth/login") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
    }
})
