package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.application.SignUpService
import com.konkuk.ma.domain.auth.application.command.SignUpCommand
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.requestPart
import com.konkuk.ma.extension.requestParts
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.memberId
import com.konkuk.ma.vocabulary.message
import com.konkuk.ma.vocabulary.nickname
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import java.time.LocalDate
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(SignUpApi::class)
@BaseApiTest
class SignUpApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val signUpService: SignUpService
) : FunSpec({

    test("signUp - 유효한 회원가입 요청시 성공한다 (사진 포함)") {
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

        val requestPart = MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            mapper.writeValueAsBytes(request)
        )

        val photoPart = MockMultipartFile(
            "photo",
            "profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake-image-content".toByteArray()
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
                ),
                any<PhotoFile>()
            )
        } returns memberId

        // When & Then
        mockMvc.multipart("/api/auth/sign-up") {
            file(requestPart)
            file(photoPart)
            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.memberId").value(memberId)
                jsonPath("$.email").value("test@example.com")
                jsonPath("$.nickname").value("testuser")
                jsonPath("$.message").value("회원가입이 완료되었습니다.")
            }
            .andDocument(
                "sign-up-with-photo",
                requestParts(
                    "request" requestPart "회원가입 정보 (JSON)",
                    "photo" requestPart "프로필 사진 파일 (선택, 10MB 이하, jpeg/jpg/png/svg/webp)" isOptional true,
                ),
                responseBody(
                    memberId(),
                    email(),
                    nickname(),
                    message(),
                )
            )
    }

    test("signUp - 사진 없이 회원가입 요청시 성공한다") {
        // Given
        val memberId = 2L
        val request = mapOf(
            "email" to "test2@example.com",
            "password" to "password123",
            "phoneNumber" to "01098765432",
            "nickname" to "testuser2",
            "gender" to Gender.FEMALE.name,
            "name" to "이테스트",
            "birthDate" to "1995-06-15",
            "region" to "BUSAN"
        )

        val requestPart = MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            mapper.writeValueAsBytes(request)
        )

        every {
            signUpService.signUp(
                SignUpCommand(
                    email = "test2@example.com",
                    password = "password123",
                    nickname = "testuser2",
                    gender = Gender.FEMALE,
                    phoneNumber = "01098765432",
                    name = "이테스트",
                    birthDate = LocalDate.of(1995, 6, 15),
                    region = Region.BUSAN,
                    highSchool = null,
                    university = null
                ),
                null
            )
        } returns memberId

        // When & Then
        mockMvc.multipart("/api/auth/sign-up") {
            file(requestPart)
            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.memberId").value(memberId)
                jsonPath("$.email").value("test2@example.com")
                jsonPath("$.nickname").value("testuser2")
            }
            .andDocument(
                "sign-up-without-photo",
                requestParts(
                    "request" requestPart "회원가입 정보 (JSON)",
                ),
                responseBody(
                    memberId(),
                    email(),
                    nickname(),
                    message(),
                )
            )
    }
})
