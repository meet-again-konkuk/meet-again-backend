package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.api.request.FindIdRequest
import com.konkuk.ma.domain.auth.application.FindIdService
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.name
import com.konkuk.ma.vocabulary.phoneNumber
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(FindIdApi::class)
@BaseApiTest
class FindIdApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val findIdService: FindIdService
) : FunSpec({

    test("이메일 찾기 API 문서화") {
        val request = FindIdRequest(name = "홍길동", phone = "01012345678")
        every { findIdService.findId(request.name, request.phone) } returns Email("holeman@naver.com")

        mockMvc.postJson("/api/auth/find-id") { content = mapper.writeValueAsString(request) }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.email").value("hol***@naver.com") }
            .andDocument(
                "auth-find-id",
                requestBody(
                    name(),
                    phoneNumber("phone"),
                ),
                responseBody(
                    email() means "마스킹된 이메일",
                )
            )
    }

    test("이메일 찾기 - 이름 형식이 올바르지 않으면 400을 반환한다") {
        val badRequest = mapOf(
            "name" to "A",
            "phone" to "01012345678"
        )

        mockMvc.postJson("/api/auth/find-id") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "auth-find-id-invalid-name",
                requestBody(
                    name() means "잘못된 이름 형식",
                    phoneNumber("phone"),
                )
            )
    }

    test("이메일 찾기 - 휴대폰 번호에 하이픈이 포함되면 400을 반환한다") {
        val badRequest = mapOf(
            "name" to "홍길동",
            "phone" to "010-1234-5678"
        )

        mockMvc.postJson("/api/auth/find-id") { content = mapper.writeValueAsString(badRequest) }
            .andExpect { status { isBadRequest() } }
    }
})
