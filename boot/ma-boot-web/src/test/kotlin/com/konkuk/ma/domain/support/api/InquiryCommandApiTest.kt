package com.konkuk.ma.domain.support.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.support.application.InquiryCommandService
import com.konkuk.ma.domain.support.domain.NewInquiry
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.inquiryContent
import com.konkuk.ma.vocabulary.inquiryId
import com.konkuk.ma.vocabulary.inquiryTitle
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(InquiryCommandApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class InquiryCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val inquiryCommandService: InquiryCommandService,
) : FunSpec({

    test("1:1 문의 접수 API 문서화") {
        // Given
        val request = mapOf(
            "title" to "로그인이 안됩니다",
            "content" to "비밀번호를 올바르게 입력해도 로그인이 되지 않습니다.",
        )

        every {
            inquiryCommandService.create(match {
                it.authorEmail == Email("test@example.com") &&
                    it.title == "로그인이 안됩니다" &&
                    it.content == "비밀번호를 올바르게 입력해도 로그인이 되지 않습니다."
            })
        } returns 1L

        // When & Then
        mockMvc.postJson("/api/domain/inquiries") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isCreated() } }
            .andDocument(
                "support/create-inquiry",
                requestBody(
                    inquiryTitle(),
                    inquiryContent(),
                ),
                responseBody(
                    inquiryId(),
                ),
            )
    }

    test("제목이 최대 글자수를 초과하면 400을 반환한다") {
        // Given
        val request = mapOf(
            "title" to "가".repeat(NewInquiry.MAX_TITLE_LENGTH + 1),
            "content" to "문의 내용",
        )

        // When & Then
        mockMvc.postJson("/api/domain/inquiries") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }

    test("내용이 최대 글자수를 초과하면 400을 반환한다") {
        // Given
        val request = mapOf(
            "title" to "문의 제목",
            "content" to "가".repeat(NewInquiry.MAX_CONTENT_LENGTH + 1),
        )

        // When & Then
        mockMvc.postJson("/api/domain/inquiries") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }

    test("제목이 비어있으면 400을 반환한다") {
        // Given
        val request = mapOf(
            "title" to "",
            "content" to "문의 내용",
        )

        // When & Then
        mockMvc.postJson("/api/domain/inquiries") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }

    test("내용이 비어있으면 400을 반환한다") {
        // Given
        val request = mapOf(
            "title" to "문의 제목",
            "content" to "",
        )

        // When & Then
        mockMvc.postJson("/api/domain/inquiries") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }
})
