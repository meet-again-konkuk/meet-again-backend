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
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
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
    private val idObfuscator: IdObfuscator,
    @MockkBean private val signUpService: SignUpService
) : FunSpec({

    test("signUp - 유효한 회원가입 요청시 성공한다") {
        // Given
        val memberId = 1L
        val encodedMemberId = idObfuscator.encode(ObfuscationType.MEMBER, memberId)
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
        mockMvc.postJson("/api/auth/sign-up") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.memberId").value(encodedMemberId)
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
                    highSchool() means "고등학교 (선택)",
                    university() means "대학교 (선택)",
                ),
                responseBody(
                    memberId(),
                    email(),
                    nickname(),
                    message(),
                )
            )
    }

    test("signUp - 선택 필드 없이 회원가입 요청시 성공한다") {
        // Given
        val memberId = 2L
        val encodedMemberId = idObfuscator.encode(ObfuscationType.MEMBER, memberId)
        val request = mapOf(
            "email" to "test2@example.com",
            "password" to "password123",
            "phoneNumber" to "01098765432",
            "nickname" to "테스터2",
            "gender" to Gender.FEMALE.name,
            "name" to "이테스트",
            "birthDate" to "1995-06-15",
            "region" to "BUSAN"
        )

        every {
            signUpService.signUp(
                SignUpCommand(
                    email = "test2@example.com",
                    password = "password123",
                    nickname = "테스터2",
                    gender = Gender.FEMALE,
                    phoneNumber = "01098765432",
                    name = "이테스트",
                    birthDate = LocalDate.of(1995, 6, 15),
                    region = Region.BUSAN,
                    highSchool = null,
                    university = null
                )
            )
        } returns memberId

        // When & Then
        mockMvc.postJson("/api/auth/sign-up") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.memberId").value(encodedMemberId)
                jsonPath("$.email").value("test2@example.com")
                jsonPath("$.nickname").value("테스터2")
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
                ),
                responseBody(
                    memberId(),
                    email(),
                    nickname(),
                    message(),
                )
            )
    }

    context("signUp - 유효성 검증 실패") {
        val validRequest = mapOf(
            "email" to "test@example.com",
            "password" to "password123",
            "phoneNumber" to "01012345678",
            "nickname" to "testuser",
            "gender" to Gender.MALE.name,
            "name" to "김테스트",
            "birthDate" to "1990-01-01",
            "region" to "SEOUL"
        )

        test("이메일이 비어있으면 400을 반환한다") {
            val request = validRequest + ("email" to "")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("유효하지 않은 이메일 형식이면 400을 반환한다") {
            val request = validRequest + ("email" to "invalid-email")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("비밀번호가 비어있으면 400을 반환한다") {
            val request = validRequest + ("password" to "")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("유효하지 않은 비밀번호 형식이면 400을 반환한다") {
            val request = validRequest + ("password" to "short1")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("휴대폰 번호가 비어있으면 400을 반환한다") {
            val request = validRequest + ("phoneNumber" to "")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("유효하지 않은 휴대폰 번호 형식이면 400을 반환한다") {
            val request = validRequest + ("phoneNumber" to "0111234567")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("유효하지 않은 닉네임 형식이면 400을 반환한다") {
            val request = validRequest + ("nickname" to "!@#invalid")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("이름이 비어있으면 400을 반환한다") {
            val request = validRequest + ("name" to "")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }

        test("유효하지 않은 이름 형식이면 400을 반환한다") {
            val request = validRequest + ("name" to "abc123")

            mockMvc.postJson("/api/auth/sign-up") {
                content = mapper.writeValueAsString(request)
            }.andExpect { status { isBadRequest() } }
        }
    }
})
