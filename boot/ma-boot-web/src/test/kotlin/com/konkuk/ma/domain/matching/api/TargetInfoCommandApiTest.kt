package com.konkuk.ma.domain.matching.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.matching.application.TargetInfoCommandService
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.UpdateTargetInfo
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.postJson
import com.konkuk.ma.extension.deleteJson
import com.konkuk.ma.extension.putJson
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.targetGender
import com.konkuk.ma.vocabulary.day
import com.konkuk.ma.vocabulary.lastNumber
import com.konkuk.ma.vocabulary.middleNumber
import com.konkuk.ma.vocabulary.month
import com.konkuk.ma.vocabulary.registerEmail
import com.konkuk.ma.vocabulary.targetInfoId
import com.konkuk.ma.vocabulary.targetName
import com.konkuk.ma.vocabulary.targetRegion
import com.konkuk.ma.vocabulary.year
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.justRun
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(TargetInfoCommandApi::class)
@BaseApiTest
class TargetInfoCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val targetInfoCommandService: TargetInfoCommandService
) : FunSpec({

    test("찾는 사람 정보 등록 API 문서화") {
        // Given
        val targetInfoId = 1L
        val encodedTargetInfoId = idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId)
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
                    it.registerEmail == Email("holeman@naver.com") &&
                    it.targetName == "김만남" &&
                    it.middleNumber?.value == "1234" &&
                    it.lastNumber?.value == "5678" &&
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
                jsonPath("$.targetInfoId").value(encodedTargetInfoId)
                jsonPath("$.name").value("김만남")
            }
            .andDocument(
                "matching/register-target-info",
                requestBody(
                    targetName(),
                    middleNumber() means "전화번호 중간자리 (선택)",
                    lastNumber() means "전화번호 뒷자리 (선택)",
                    year() means "생년 (선택)",
                    month() means "생월 (선택)",
                    day() means "생일 (선택)",
                    targetRegion() means "지역 (선택)",
                ),
                responseBody(
                    targetInfoId(),
                    registerEmail(),
                )
            )
    }

    test("찾는 사람 정보 등록 - 필수 정보만 입력") {
        // Given
        val targetInfoId = 2L
        val encodedTargetInfoId = idObfuscator.encode(ObfuscationType.TARGET_INFO, targetInfoId)
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
                    it.registerEmail == Email("holeman@naver.com") &&
                    it.targetName == "이재회" &&
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
                jsonPath("$.targetInfoId").value(encodedTargetInfoId)
                jsonPath("$.name").value("이재회")
            }
            .andDocument(
                "matching/register-target-info-minimal",
                requestBody(
                    targetName() means "찾는 사람의 이름 (필수)",
                    middleNumber() means "전화번호 중간자리 (선택, null)",
                    lastNumber() means "전화번호 뒷자리 (선택, null)",
                    year() means "생년 (선택, null)",
                    month() means "생월 (선택, null)",
                    day() means "생일 (선택, null)",
                    targetRegion() means "지역 (선택, null)",
                ),
                responseBody(
                    targetInfoId(),
                    registerEmail(),
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
                    targetName() means "잘못된 이름 형식 (한글 2-10자)",
                    middleNumber(),
                    lastNumber(),
                )
            )
    }

    test("찾는 사람 정보 수정 API 문서화") {
        // Given
        val id = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.TARGET_INFO, id)
        val request = mapOf(
            "name" to "박수정",
            "middleNumber" to "4321",
            "lastNumber" to "8765",
            "year" to 1996,
            "month" to 3,
            "day" to 20,
            "region" to "BUSAN",
        )

        every {
            targetInfoCommandService.update(id, "holeman@naver.com", any<UpdateTargetInfo>())
        } returns TargetInfo(
            targetInfoId = id,
            registerEmail = Email("holeman@naver.com"),
            targetName = "박수정",
            targetGender = Gender.FEMALE,
            middleNumber = FourDigit("4321"),
            lastNumber = FourDigit("8765"),
            year = Year(1996),
            month = Month(3),
            day = Day(20),
            region = Region.BUSAN,
        )

        // When & Then
        mockMvc.putJson("/api/target-infos/$encodedId") {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/update-target-info",
                requestBody(
                    targetName(),
                    middleNumber() isOptional true,
                    lastNumber() isOptional true,
                    year() isOptional true,
                    month() isOptional true,
                    day() isOptional true,
                    targetRegion() isOptional true,
                ),
                responseBody(
                    targetInfoId(),
                    targetName("targetName"),
                    targetGender(),
                    middleNumber() isOptional true,
                    lastNumber() isOptional true,
                    year() isOptional true,
                    month() isOptional true,
                    day() isOptional true,
                    targetRegion() isOptional true,
                ),
            )
    }

    test("찾는 사람 정보 삭제 API 문서화") {
        // Given
        val id = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.TARGET_INFO, id)

        justRun { targetInfoCommandService.delete(id, "holeman@naver.com") }

        // When & Then
        mockMvc.deleteJson("/api/target-infos/$encodedId")
            .andExpect { status { isNoContent() } }
            .andDocument("matching/delete-target-info")
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
                    targetName(),
                    year() means "잘못된 연도 (1900-현재)",
                    month() means "잘못된 월 (1-12)",
                    day() means "잘못된 일 (1-31)",
                )
            )
    }
})
