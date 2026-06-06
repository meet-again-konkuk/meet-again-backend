package com.konkuk.ma.domain.member.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.application.WithdrawalCancelService
import com.konkuk.ma.domain.auth.application.WithdrawalService
import com.konkuk.ma.domain.auth.application.result.WithdrawalCancelResult
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.api.request.WithdrawalRequest
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.accessToken
import com.konkuk.ma.vocabulary.cancelledAt
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.nickname
import com.konkuk.ma.vocabulary.password
import com.konkuk.ma.vocabulary.refreshToken
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import java.time.LocalDateTime
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(MemberWithdrawalApi::class)
@BaseApiTest
class MemberWithdrawalApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val withdrawalService: WithdrawalService,
    @MockkBean private val withdrawalCancelService: WithdrawalCancelService
) : FunSpec({

    test("회원 탈퇴 신청 API 문서화") {
        val request = WithdrawalRequest(password = "password1")
        every { withdrawalService.requestWithdrawal(any()) } just runs

        mockMvc.postJson("/api/members/me/withdrawal") { content = mapper.writeValueAsString(request) }
            .andExpect { status { isNoContent() } }
            .andDocument(
                "member-withdrawal-request",
                requestBody(
                    password() means "비밀번호 재확인"
                )
            )
    }

    test("회원 탈퇴 신청 - 비밀번호가 비어있으면 400을 반환한다") {
        val badRequest = mapOf("password" to "")

        mockMvc.postJson("/api/members/me/withdrawal") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
    }

    test("회원 탈퇴 복구 API 문서화") {
        val email = Email("user@example.com")
        val loginInfo = LoginInfo(
            email = email,
            nickname = "tester",
            accessToken = "new-access",
            refreshToken = RefreshToken(email, LocalDateTime.now().plusDays(7), "new-refresh")
        )
        val result = WithdrawalCancelResult(loginInfo, LocalDateTime.now())
        every { withdrawalCancelService.cancel(any()) } returns result

        mockMvc.deleteJson("/api/members/me/withdrawal")
            .andExpect { status { isOk() } }
            .andDocument(
                "member-withdrawal-cancel",
                responseBody(
                    email(),
                    nickname(),
                    cancelledAt(),
                    accessToken(),
                    refreshToken()
                )
            )
    }
})
