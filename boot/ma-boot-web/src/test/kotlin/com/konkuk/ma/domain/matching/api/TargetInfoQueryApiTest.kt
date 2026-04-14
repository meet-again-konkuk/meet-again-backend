package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.matching.application.TargetInfoQueryService
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.myDay
import com.konkuk.ma.vocabulary.myLastNumber
import com.konkuk.ma.vocabulary.myMiddleNumber
import com.konkuk.ma.vocabulary.myMonth
import com.konkuk.ma.vocabulary.myTargetGender
import com.konkuk.ma.vocabulary.myTargetInfoId
import com.konkuk.ma.vocabulary.myTargetName
import com.konkuk.ma.vocabulary.myTargetRegion
import com.konkuk.ma.vocabulary.myYear
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(TargetInfoQueryApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class TargetInfoQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val targetInfoQueryService: TargetInfoQueryService,
) : FunSpec({

    test("내가 등록한 찾는 사람 목록 조회 API 문서화") {
        // Given
        every { targetInfoQueryService.find("test@example.com") } returns listOf(
            TargetInfo(
                targetInfoId = 1L,
                registerEmail = Email("test@example.com"),
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
                    myTargetInfoId(),
                    myTargetName(),
                    myTargetGender(),
                    myMiddleNumber(),
                    myLastNumber(),
                    myYear(),
                    myMonth(),
                    myDay(),
                    myTargetRegion(),
                ),
            )
    }

    test("등록한 찾는 사람이 없으면 빈 목록을 반환한다") {
        // Given
        every { targetInfoQueryService.find("test@example.com") } returns emptyList()

        // When & Then
        mockMvc.getJson("/api/target-infos") {}
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$") { isArray() } }
            .andExpect { jsonPath("$.length()") { value(0) } }
    }
})
