package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.application.SmsVerificationService
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.phoneNumber
import com.konkuk.ma.vocabulary.verificationCode
import com.konkuk.ma.vocabulary.verified
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(SmsVerificationApi::class)
@BaseApiTest
class SmsVerificationApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val smsVerificationService: SmsVerificationService
) : FunSpec({

    test("SMS 인증 코드 발송 API 문서화") {
        // Given
        val request = mapOf(
            "phoneNumber" to "01012345678"
        )

        every { smsVerificationService.sendSmsVerificationCode("01012345678") } just runs

        // When & Then
        mockMvc.postJson("/api/sms/verification-code")
        { content = mapper.writeValueAsString(request) }
            .andExpect {
                status { isOk() }
            }
            .andDocument(
                "sms-send-verification-code",
                requestBody(
                    phoneNumber(),
                )
            )
    }

    test("유효하지 않은 휴대폰 번호로 SMS 발송 요청 시 실패") {
        // Given
        val request = mapOf(
            "phoneNumber" to "010123456789" // 11자리
        )

        // When & Then
        mockMvc.postJson("/api/sms/verification-code")
        { content = mapper.writeValueAsString(request) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "sms-send-verification-code-invalid-phone",
                requestBody(
                    phoneNumber() means "유효하지 않은 휴대폰 번호 형식",
                )
            )
    }

    test("SMS 인증 코드 확인 API 문서화 - 성공") {
        // Given
        val request = mapOf(
            "phoneNumber" to "01012345678",
            "verificationCode" to "123456"
        )

        every { smsVerificationService.confirmVerificationCode("01012345678", "123456") } returns true

        // When & Then
        mockMvc.postJson("/api/sms/verification-code/confirm")
        { content = mapper.writeValueAsString(request) }
            .andExpect {
                status { isOk() }
                jsonPath("$.phoneNumber").value("01012345678")
                jsonPath("$.verificationCode").value("123456")
                jsonPath("$.verified").value(true)
            }
            .andDocument(
                "sms-confirm-verification-code-success",
                requestBody(
                    phoneNumber(),
                    verificationCode(),
                ),
                responseBody(
                    phoneNumber(),
                    verificationCode(),
                    verified(),
                )
            )
    }

    test("SMS 인증 코드 확인 API 문서화 - 실패") {
        // Given
        val request = mapOf(
            "phoneNumber" to "01012345678",
            "verificationCode" to "999999"
        )

        every { smsVerificationService.confirmVerificationCode("01012345678", "999999") } returns false

        // When & Then
        mockMvc.postJson("/api/sms/verification-code/confirm")
        { content = mapper.writeValueAsString(request) }
            .andExpect {
                status { isOk() }
                jsonPath("$.phoneNumber").value("01012345678")
                jsonPath("$.verificationCode").value("999999")
                jsonPath("$.verified").value(false)
            }
            .andDocument(
                "sms-confirm-verification-code-failure",
                requestBody(
                    phoneNumber(),
                    verificationCode() means "잘못된 인증 코드",
                ),
                responseBody(
                    phoneNumber(),
                    verificationCode(),
                    verified() means "인증 실패 여부",
                )
            )
    }

    test("유효하지 않은 휴대폰 번호로 SMS 인증 확인 요청 시 실패") {
        // Given
        val request = mapOf(
            "phoneNumber" to "invalid-phone",
            "verificationCode" to "123456"
        )

        // When & Then
        mockMvc.postJson("/api/sms/verification-code/confirm")
        { content = mapper.writeValueAsString(request) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "sms-confirm-verification-code-invalid-phone",
                requestBody(
                    phoneNumber() means "유효하지 않은 휴대폰 번호 형식",
                    verificationCode(),
                )
            )
    }
})
