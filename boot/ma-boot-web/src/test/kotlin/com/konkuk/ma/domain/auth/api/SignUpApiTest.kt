package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.application.SignUpService
import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.birthDate
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.gender
import com.konkuk.ma.vocabulary.highSchool
import com.konkuk.ma.vocabulary.memberId
import com.konkuk.ma.vocabulary.message
import com.konkuk.ma.vocabulary.name
import com.konkuk.ma.vocabulary.nickname
import com.konkuk.ma.vocabulary.password
import com.konkuk.ma.vocabulary.phoneNumber
import com.konkuk.ma.vocabulary.region
import com.konkuk.ma.vocabulary.university
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import java.time.LocalDate
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(SignUpApi::class)
@BaseApiTest
class SignUpApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val signUpService: SignUpService
) : FunSpec({

    test("signUp - 유효한 회원가입 요청시 성공한다") {
        // Given
        val memberId = 1L
        val request = mapOf(
            "email" to "test@example.com",
            "password" to "password123",
            "phoneNumber" to "01012345678",
            "nickname" to "testuser",
            "gender" to Gender.MALE.name,
            "name" to "김테스트",
            "birthDate" to "1990-01-01",
            "region" to "SEOUL",
            "highSchool" to "테스트고등학교",
            "university" to "테스트대학교"
        )

        every {
            signUpService.signUp(
                SignUpCommand(
                    email = "test@example.com",
                    password = "password123",
                    nickname = "testuser",
                    gender = Gender.MALE,
                    phoneNumber = "01012345678",
                    name = "김테스트",
                    birthDate = LocalDate.of(1990, 1, 1),
                    region = Region.SEOUL,
                    highSchool = "테스트고등학교",
                    university = "테스트대학교"
                )
            )
        } returns memberId

        // When & Then
        mockMvc.postJson("/api/auth/sign-up")
        { content = mapper.writeValueAsString(request) }
            .andExpect {
                status { isCreated() }
                jsonPath("$.memberId").value(memberId)
                jsonPath("$.email").value("test@example.com")
                jsonPath("$.nickname").value("testuser")
                jsonPath("$.message").value("회원가입이 완료되었습니다.")
            }
            .andDocument(
                "sign-up",
                requestBody(
                    email(),
                    password(),
                    phoneNumber(),
                    nickname(),
                    gender(),
                    name(),
                    birthDate(),
                    region(),
                    highSchool(),
                    university(),
                ),
                responseBody(
                    memberId(),
                    email(),
                    nickname(),
                    message(),
                )
            )
    }

    test("signUp - 유효하지 않은 이메일 형식으로 요청시 실패한다") {
        // Given
        val request = mapOf(
            "email" to "invalid-email",
            "password" to "password123",
            "phoneNumber" to "01012345678",
            "nickname" to "testuser",
            "name" to "김테스트",
            "birthDate" to "1990-01-01"
        )

        // When & Then
        mockMvc.postJson("/api/auth/sign-up")
        { content = mapper.writeValueAsString(request) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "sign-up-invalid-email",
                requestBody(
                    email() means "잘못된 이메일 형식",
                    password(),
                    phoneNumber(),
                    nickname(),
                    name(),
                    birthDate(),
                )
            )
    }

    test("signUp - 유효하지 않은 비밀번호 형식으로 요청시 실패한다") {
        // Given
        val request = mapOf(
            "email" to "test@example.com",
            "password" to "weak", // 너무 짧고 숫자 없음
            "phoneNumber" to "01012345678",
            "nickname" to "testuser",
            "name" to "김테스트",
            "birthDate" to "1990-01-01"
        )

        // When & Then
        mockMvc.postJson("/api/auth/sign-up")
        { content = mapper.writeValueAsString(request) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "sign-up-invalid-password",
                requestBody(
                    email(),
                    password() means "잘못된 비밀번호 형식",
                    phoneNumber(),
                    nickname(),
                    name(),
                    birthDate(),
                )
            )
    }
})
