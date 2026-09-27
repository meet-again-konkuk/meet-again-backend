package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.application.TargetInfoQueryService
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.day
import com.konkuk.ma.vocabulary.lastNumber
import com.konkuk.ma.vocabulary.middleNumber
import com.konkuk.ma.vocabulary.month
import com.konkuk.ma.vocabulary.targetGender
import com.konkuk.ma.vocabulary.targetInfoId
import com.konkuk.ma.vocabulary.targetName
import com.konkuk.ma.vocabulary.targetRegion
import com.konkuk.ma.vocabulary.year
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(TargetInfoQueryApi::class)
@BaseApiTest
class TargetInfoQueryApiTest(
    private val mockMvc: MockMvc,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val targetInfoQueryService: TargetInfoQueryService,
) : FunSpec({

    val authMemberId = 1L

    test("내가 등록한 찾는 사람 목록 조회 API 문서화") {
        // Given
        every { targetInfoQueryService.find(authMemberId) } returns listOf(
            TargetInfo(
                targetInfoId = 1L,
                registerId = authMemberId,
                targetName = "김만남",
                targetGender = Gender.FEMALE,
                middleNumber = FourDigit("1234"),
                lastNumber = FourDigit("5678"),
                year = Year(1995),
                month = Month(5),
                day = Day(15),
                region = Region.SEOUL,
            ),
        )

        // When & Then
        mockMvc.getJson("/api/target-infos") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-my-target-infos",
                responseBody(
                    targetInfoId("[].targetInfoId"),
                    targetName("[].targetName"),
                    targetGender("[].targetGender"),
                    middleNumber("[].middleNumber") isOptional true,
                    lastNumber("[].lastNumber") isOptional true,
                    year("[].year") isOptional true,
                    month("[].month") isOptional true,
                    day("[].day") isOptional true,
                    targetRegion("[].region") isOptional true,
                ),
            )
    }

    test("등록한 찾는 사람이 없으면 빈 목록을 반환한다") {
        // Given
        every { targetInfoQueryService.find(authMemberId) } returns emptyList()

        // When & Then
        mockMvc.getJson("/api/target-infos") {}
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$") { isArray() } }
            .andExpect { jsonPath("$.length()") { value(0) } }
    }

    test("찾는 사람 상세 조회 API 문서화") {
        // Given
        val encryptedId = idObfuscator.encode(ObfuscationType.TARGET_INFO, 1L)

        every { targetInfoQueryService.findDetail(1L, authMemberId) } returns TargetInfo(
            targetInfoId = 1L,
            registerId = authMemberId,
            targetName = "김만남",
            targetGender = Gender.FEMALE,
            middleNumber = FourDigit("1234"),
            lastNumber = FourDigit("5678"),
            year = Year(1995),
            month = Month(5),
            day = Day(15),
            region = Region.SEOUL,
        )

        // When & Then
        mockMvc.getJson("/api/target-infos/$encryptedId") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-target-info-detail",
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
})
