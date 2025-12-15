package com.konkuk.ma.domain.matching.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.matching.application.TargetInfoCommandService
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.extension.responseType
import com.konkuk.ma.support.security.WithAuthMember
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(TargetInfoCommandApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class TargetInfoCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val targetInfoCommandService: TargetInfoCommandService
) : FunSpec({

    test("찾는 사람 정보 등록 API 문서화") {
        // Given
        val targetInfoId = 1L
        val request = mapOf(
            "name" to "김만남",
            "middleNumber" to "1234",
            "lastNumber" to "5678",
            "year" to 1995,
            "month" to 5,
            "day" to 15,
            "region" to "SEOUL"
        )

        every {
            targetInfoCommandService.register(
                match {
                    it.registerEmail == "test@example.com" &&
                    it.name == "김만남" &&
                    it.middleNumber == "1234" &&
                    it.lastNumber == "5678" &&
                    it.year?.value == 1995 &&
                    it.month?.value == 5 &&
                    it.day?.value == 15 &&
                    it.region == Region.SEOUL
                }
            )
        } returns targetInfoId

        // When & Then
        mockMvc.postJson("/api/target-infos") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.targetInfoId").value(targetInfoId)
                jsonPath("$.name").value("김만남")
            }
            .andDocument(
                "matching/register-target-info",
                requestBody(
                    "name" responseType STRING means "찾는 사람의 이름",
                    "middleNumber" responseType STRING means "전화번호 중간자리 (선택)",
                    "lastNumber" responseType STRING means "전화번호 뒷자리 (선택)",
                    "year" responseType NUMBER means "생년 (선택)",
                    "month" responseType NUMBER means "생월 (선택)",
                    "day" responseType NUMBER means "생일 (선택)",
                    "region" responseType STRING means "지역 (선택)"
                ),
                responseBody(
                    "targetInfoId" responseType NUMBER means "등록된 찾는 사람 정보 ID",
                    "email" responseType STRING means "등록자 email"
                )
            )
    }

    test("찾는 사람 정보 등록 - 필수 정보만 입력") {
        // Given
        val targetInfoId = 2L
        val request = mapOf(
            "name" to "이재회",
            "middleNumber" to null,
            "lastNumber" to null,
            "year" to null,
            "month" to null,
            "day" to null,
            "region" to null
        )

        every {
            targetInfoCommandService.register(
                match {
                    it.registerEmail == "test@example.com" &&
                    it.name == "이재회" &&
                    it.middleNumber == null &&
                    it.lastNumber == null &&
                    it.year == null &&
                    it.month == null &&
                    it.day == null &&
                    it.region == null
                }
            )
        } returns targetInfoId

        // When & Then
        mockMvc.postJson("/api/target-infos") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.targetInfoId").value(targetInfoId)
                jsonPath("$.name").value("이재회")
            }
            .andDocument(
                "matching/register-target-info-minimal",
                requestBody(
                    "name" responseType STRING means "찾는 사람의 이름 (필수)",
                    "middleNumber" responseType STRING means "전화번호 중간자리 (선택, null)",
                    "lastNumber" responseType STRING means "전화번호 뒷자리 (선택, null)",
                    "year" responseType NUMBER means "생년 (선택, null)",
                    "month" responseType NUMBER means "생월 (선택, null)",
                    "day" responseType NUMBER means "생일 (선택, null)",
                    "region" responseType STRING means "지역 (선택, null)"
                ),
                responseBody(
                    "targetInfoId" responseType NUMBER means "등록된 찾는 사람 정보 ID",
                    "email" responseType STRING means "등록자 email"
                )
            )
    }

    test("찾는 사람 정보 등록 - 유효하지 않은 이름 형식") {
        // Given
        val request = mapOf(
            "name" to "a", // 너무 짧음
            "middleNumber" to "1234",
            "lastNumber" to "5678"
        )

        // When & Then
        mockMvc.postJson("/api/target-infos") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "matching/register-target-info-invalid-name",
                requestBody(
                    "name" responseType STRING means "잘못된 이름 형식 (한글 2-10자)",
                    "middleNumber" responseType STRING means "전화번호 중간자리",
                    "lastNumber" responseType STRING means "전화번호 뒷자리"
                )
            )
    }

    test("찾는 사람 정보 등록 - 유효하지 않은 생년월일") {
        // Given
        val request = mapOf(
            "name" to "김만남",
            "year" to 1800, // 너무 오래된 연도
            "month" to 13, // 잘못된 월
            "day" to 32 // 잘못된 일
        )

        // When & Then
        mockMvc.postJson("/api/target-infos")
        { content = mapper.writeValueAsString(request) }
            .andExpect { status { isBadRequest() } }
            .andDocument(
                "matching/register-target-info-invalid-date",
                requestBody(
                    "name" responseType STRING means "찾는 사람의 이름",
                    "year" responseType NUMBER means "잘못된 연도 (1900-현재)",
                    "month" responseType NUMBER means "잘못된 월 (1-12)",
                    "day" responseType NUMBER means "잘못된 일 (1-31)"
                )
            )
    }
})
