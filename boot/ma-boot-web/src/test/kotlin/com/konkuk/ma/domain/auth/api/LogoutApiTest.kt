package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.auth.JwtManager
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.application.LogoutService
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.vocabulary.authorizationHeader
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(LogoutApi::class)
@BaseApiTest
class LogoutApiTest(
    private val mockMvc: MockMvc,
    private val jwtManager: JwtManager,
    @MockkBean private val logoutService: LogoutService,
) : FunSpec({

    test("로그아웃 API 문서화") {
        // Given - 인증된 회원(id=1)의 유효한 액세스 토큰
        val accessToken = jwtManager.generateAccessToken(1L)
        every { logoutService.logout(any()) } just runs

        // When & Then
        mockMvc.postJson("/api/auth/logout") {
            authorization("Bearer $accessToken")
        }
            .andExpect { status { isNoContent() } }
            .andDocument(
                "auth-logout",
                authorizationHeader(),
            )
    }
})
