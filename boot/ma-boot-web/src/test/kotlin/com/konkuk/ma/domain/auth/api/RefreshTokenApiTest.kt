package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.application.RefreshTokenService
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.api.request.RefreshTokenRequest
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.accessToken
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.nickname
import com.konkuk.ma.vocabulary.refreshToken
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import java.time.LocalDateTime
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(RefreshTokenApi::class)
@BaseApiTest
class RefreshTokenApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val refreshTokenService: RefreshTokenService
) : FunSpec({

    test("리프레시 토큰 재발급 API 문서화") {
        val request = RefreshTokenRequest(refreshToken = "old-refresh-token")
        val loginInfo = LoginInfo(
            email = "user@example.com",
            nickname = "tester",
            accessToken = "new-access-token",
            refreshToken = RefreshToken("user@example.com", LocalDateTime.now().plusDays(7), "new-refresh-token")
        )
        every { refreshTokenService.refreshToken(request.refreshToken) } returns loginInfo

        mockMvc.postJson("/api/auth/refresh-token") { content = mapper.writeValueAsString(request) }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.accessToken").value("new-access-token") }
            .andExpect { jsonPath("$.refreshToken.token").value("new-refresh-token") }
            .andDocument(
                "auth-refresh-token",
                requestBody(
                    refreshToken() means "기존 리프레시 토큰",
                ),
                responseBody(
                    email(),
                    nickname(),
                    accessToken() means "새 액세스 토큰",
                    refreshToken() means "새 리프레시 토큰",
                )
            )
    }

    test("리프레시 토큰 - 공백 토큰으로 요청 시 실패한다") {
        val badRequest = mapOf(
            "refreshToken" to ""
        )

        mockMvc.postJson("/api/auth/refresh-token") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "auth-refresh-token-invalid",
                requestBody(
                    refreshToken() means "빈 값 허용 안 함",
                )
            )
    }
})
