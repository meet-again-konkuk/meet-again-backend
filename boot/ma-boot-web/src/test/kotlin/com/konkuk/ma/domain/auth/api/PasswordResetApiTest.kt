package com.konkuk.ma.domain.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.auth.api.request.PasswordResetRequest
import com.konkuk.ma.domain.auth.application.PasswordResetService
import com.konkuk.ma.domain.member.exception.SmsNotVerifiedException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.vocabulary.email
import com.konkuk.ma.vocabulary.name
import com.konkuk.ma.vocabulary.password
import com.konkuk.ma.vocabulary.phoneNumber
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

/**
 * 비밀번호 재설정(POST /api/auth/find-password) API 슬라이스 테스트.
 *
 * PasswordResetService 는 @MockkBean 으로 대체하여 컨트롤러 계약(HTTP 매핑·상태코드·Bean Validation)만 검증한다.
 * 정상/예외 위임 케이스에는 REST Docs(andDocument) 스니펫(auth-find-password*)을 함께 생성한다.
 *
 * 확정 계약(이 테스트가 명세):
 *  - 정상 요청 → 204 No Content (본문 없음).
 *  - Service 가 SmsNotVerifiedException → 400 (SMS 인증 미완료).
 *  - Service 가 EntityNotFoundException → 404 (3필드 일치 회원 부재).
 *  - Bean Validation 위반(email/name/phone/newPassword 형식·필수) → 400, Service 도달 이전에 차단.
 */
@WebMvcTest(PasswordResetApi::class)
@BaseApiTest
class PasswordResetApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val passwordResetService: PasswordResetService,
) : FunSpec({

    val uri = "/api/auth/find-password"

    // 유효한 기본값 — 관심 있는 한 필드만 오버라이드해 Bean Validation 위반을 만든다.
    val validEmail = "holeman@naver.com"
    val validName = "홍길동"
    val validPhone = "01012345678"
    val validNewPassword = "password1"

    fun body(
        email: String = validEmail,
        name: String = validName,
        phone: String = validPhone,
        newPassword: String = validNewPassword,
    ) = mapOf(
        "email" to email,
        "name" to name,
        "phone" to phone,
        "newPassword" to newPassword,
    )

    context("정상/예외 위임 (Service 결과에 따른 상태코드)") {

        test("유효한 요청이면 204 No Content 를 반환한다") {
            // Given - 4필드 raw String 위임, Service 는 정상 수행
            val request = PasswordResetRequest(validEmail, validName, validPhone, validNewPassword)
            every {
                passwordResetService.resetPassword(request.email, request.name, request.phone, request.newPassword)
            } just runs

            // When & Then
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(request) }
                .andExpect { status { isNoContent() } }
                .andDocument(
                    "auth-find-password",
                    requestBody(
                        email() means "회원 이메일 (아이디)",
                        name(),
                        phoneNumber("phone"),
                        password("newPassword") means "새 비밀번호 (영문+숫자 8~16자)",
                    )
                )
        }

        test("Service 가 SmsNotVerifiedException 을 던지면 400 을 반환한다") {
            // Given - SMS 인증 미완료
            val request = PasswordResetRequest(validEmail, validName, validPhone, validNewPassword)
            every {
                passwordResetService.resetPassword(request.email, request.name, request.phone, request.newPassword)
            } throws SmsNotVerifiedException(request.phone)

            // When & Then
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(request) }
                .andExpect { status { isBadRequest() } }
                .andDocument(
                    "auth-find-password-sms-not-verified",
                    requestBody(
                        email() means "회원 이메일 (아이디)",
                        name(),
                        phoneNumber("phone"),
                        password("newPassword") means "새 비밀번호 (영문+숫자 8~16자)",
                    )
                )
        }

        test("Service 가 EntityNotFoundException 을 던지면 404 를 반환한다") {
            // Given - 3필드 일치 회원 부재
            val request = PasswordResetRequest(validEmail, validName, validPhone, validNewPassword)
            every {
                passwordResetService.resetPassword(request.email, request.name, request.phone, request.newPassword)
            } throws EntityNotFoundException(EntityType.MEMBER, request.email)

            // When & Then
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(request) }
                .andExpect { status { isNotFound() } }
                .andDocument(
                    "auth-find-password-not-found",
                    requestBody(
                        email() means "회원 이메일 (아이디)",
                        name(),
                        phoneNumber("phone"),
                        password("newPassword") means "새 비밀번호 (영문+숫자 8~16자)",
                    )
                )
        }
    }

    context("이메일 형식 검증 (bean validation → 400)") {

        test("이메일이 빈 값이면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(email = "")) }
                .andExpect { status { isBadRequest() } }
        }

        test("이메일 형식이 올바르지 않으면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(email = "not-an-email")) }
                .andExpect { status { isBadRequest() } }
        }
    }

    context("이름 형식 검증 (bean validation → 400)") {

        test("이름이 빈 값이면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(name = "")) }
                .andExpect { status { isBadRequest() } }
        }

        test("이름이 1글자면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(name = "김")) }
                .andExpect { status { isBadRequest() } }
        }

        test("이름이 11글자면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(name = "김".repeat(11))) }
                .andExpect { status { isBadRequest() } }
        }

        test("이름이 영문이면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(name = "John")) }
                .andExpect { status { isBadRequest() } }
        }

        test("이름에 특수문자가 포함되면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(name = "김철수!")) }
                .andExpect { status { isBadRequest() } }
        }
    }

    context("전화번호 형식 검증 (bean validation → 400)") {

        test("전화번호가 빈 값이면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(phone = "")) }
                .andExpect { status { isBadRequest() } }
        }

        test("전화번호에 하이픈이 포함되면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(phone = "010-1234-5678")) }
                .andExpect { status { isBadRequest() } }
        }

        test("전화번호가 010으로 시작하지 않으면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(phone = "01112345678")) }
                .andExpect { status { isBadRequest() } }
        }

        test("전화번호 자릿수가 부족하면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(phone = "010123456")) }
                .andExpect { status { isBadRequest() } }
        }

        test("전화번호 자릿수가 초과되면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(phone = "010123456789")) }
                .andExpect { status { isBadRequest() } }
        }
    }

    context("새 비밀번호 형식 검증 (bean validation → 400)") {

        test("새 비밀번호가 빈 값이면 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(newPassword = "")) }
                .andExpect { status { isBadRequest() } }
        }

        test("새 비밀번호가 8자 미만이면 400을 반환한다") {
            // "pass1" = 5자
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(newPassword = "pass1")) }
                .andExpect { status { isBadRequest() } }
        }

        test("새 비밀번호가 16자를 초과하면 400을 반환한다") {
            // 영문 16자 + 숫자 1자 = 17자
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(newPassword = "a".repeat(16) + "1")) }
                .andExpect { status { isBadRequest() } }
        }

        test("새 비밀번호가 문자만 있으면(숫자 없음) 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(newPassword = "password")) }
                .andExpect { status { isBadRequest() } }
        }

        test("새 비밀번호가 숫자만 있으면(문자 없음) 400을 반환한다") {
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(newPassword = "12345678")) }
                .andExpect { status { isBadRequest() } }
        }

        test("새 비밀번호에 특수문자가 포함되면 400을 반환한다") {
            // 패턴 [A-Za-z\d] 는 특수문자를 허용하지 않는다
            mockMvc.postJson(uri) { content = mapper.writeValueAsString(body(newPassword = "password1!")) }
                .andExpect { status { isBadRequest() } }
        }
    }
})
