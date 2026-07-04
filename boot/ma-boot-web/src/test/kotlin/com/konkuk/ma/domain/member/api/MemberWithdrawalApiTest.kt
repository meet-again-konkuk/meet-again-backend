package com.konkuk.ma.domain.member.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.application.WithdrawalCancelService
import com.konkuk.ma.domain.auth.application.WithdrawalService
import com.konkuk.ma.domain.member.api.request.WithdrawalCancelRequest
import com.konkuk.ma.domain.member.api.request.WithdrawalRequest
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.password
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
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
        every { withdrawalService.requestWithdrawal(any(), any()) } just runs

        mockMvc.postJson("/api/members/withdrawal") { content = mapper.writeValueAsString(request) }
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

        mockMvc.postJson("/api/members/withdrawal") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
    }

    test("회원 탈퇴 복구 API 문서화") {
        val request = WithdrawalCancelRequest(email = "user@example.com", password = "password1")
        every { withdrawalCancelService.cancel(any(), any()) } just runs

        mockMvc.postJson("/api/members/withdrawal/cancellation") { content = mapper.writeValueAsString(request) }
            .andExpect { status { isNoContent() } }
            .andDocument(
                "member-withdrawal-cancel",
                requestBody(
                    email() means "이메일",
                    password() means "비밀번호"
                )
            )
    }

    test("회원 탈퇴 복구 - 이메일이 비어있으면 400을 반환한다") {
        val badRequest = mapOf("email" to "", "password" to "password1")

        mockMvc.postJson("/api/members/withdrawal/cancellation") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
    }
})
