package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.claimed
import com.konkuk.ma.vocabulary.dayMatched
import com.konkuk.ma.vocabulary.detailMatchRate
import com.konkuk.ma.vocabulary.detailMatchingResultId
import com.konkuk.ma.vocabulary.detailRemainingDays
import com.konkuk.ma.vocabulary.lastNumberMatched
import com.konkuk.ma.vocabulary.matchRate
import com.konkuk.ma.vocabulary.matchingResultId
import com.konkuk.ma.vocabulary.memberId
import com.konkuk.ma.vocabulary.memberIsWithdrawn
import com.konkuk.ma.vocabulary.memberName
import com.konkuk.ma.vocabulary.memberNickname
import com.konkuk.ma.vocabulary.memberProfileImageUrl
import com.konkuk.ma.vocabulary.middleNumberMatched
import com.konkuk.ma.vocabulary.monthMatched
import com.konkuk.ma.vocabulary.regionMatched
import com.konkuk.ma.vocabulary.remainingDays
import com.konkuk.ma.vocabulary.resultExcludedParam
import com.konkuk.ma.vocabulary.yearMatched
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(MatchingResultQueryApi::class)
@BaseApiTest
class MatchingResultQueryApiTest(
    private val mockMvc: MockMvc,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val matchingResultQueryService: MatchingResultQueryService
) : FunSpec({

    test("매칭 결과 목록 조회 API 문서화") {
        // Given
        val matchingResult = MatchingResultFixture.create(
            registerEmail = "holeman@naver.com",
            monthMatched = false,
            dayMatched = false,
        )
        val resultsWithProfiles = MatchingResultsWithProfiles(
            data = listOf(
                MatchingResultWithProfile(
                    matchingResult = matchingResult,
                    targetMemberId = 1L,
                    targetName = "김만남",
                    targetNickname = "테스트닉네임",
                    profileImageUrl = "https://example.com/image.jpg",
                )
            )
        )

        every { matchingResultQueryService.find("holeman@naver.com", false) } returns resultsWithProfiles

        // When & Then
        mockMvc.getJson("/api/matching-results") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-matching-results",
                requestParam(
                    resultExcludedParam(),
                ),
                responseBody(
                    matchingResultId(),
                    memberId("matchingResults[].targetMemberId"),
                    memberName("matchingResults[].targetName"),
                    memberNickname("matchingResults[].targetNickname"),
                    memberProfileImageUrl("matchingResults[].profileImageUrl"),
                    remainingDays(),
                    matchRate(),
                    memberIsWithdrawn("matchingResults[].isWithdrawn"),
                    claimed(),
                )
            )
    }

    test("excluded=true로 제외된 매칭 결과 목록 조회 API 문서화") {
        // Given
        val matchingResult = MatchingResultFixture.create(
            registerEmail = "holeman@naver.com",
            monthMatched = false,
            dayMatched = false,
        )
        val resultsWithProfiles = MatchingResultsWithProfiles(
            data = listOf(
                MatchingResultWithProfile(
                    matchingResult = matchingResult,
                    targetMemberId = 1L,
                    targetName = "김만남",
                    targetNickname = "테스트닉네임",
                    profileImageUrl = "https://example.com/image.jpg",
                )
            )
        )

        every { matchingResultQueryService.find("holeman@naver.com", true) } returns resultsWithProfiles

        // When & Then
        mockMvc.getJson("/api/matching-results") {
            param("excluded", "true")
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-excluded-matching-results",
                requestParam(
                    resultExcludedParam(),
                ),
                responseBody(
                    matchingResultId(),
                    memberId("matchingResults[].targetMemberId"),
                    memberName("matchingResults[].targetName"),
                    memberNickname("matchingResults[].targetNickname"),
                    memberProfileImageUrl("matchingResults[].profileImageUrl"),
                    remainingDays(),
                    matchRate(),
                    memberIsWithdrawn("matchingResults[].isWithdrawn"),
                    claimed(),
                )
            )
    }

    test("매칭 결과가 없는 경우 빈 목록 반환") {
        // Given
        val emptyResults = MatchingResultsWithProfiles(data = emptyList())

        every { matchingResultQueryService.find("holeman@naver.com", false) } returns emptyResults

        // When & Then
        mockMvc.getJson("/api/matching-results") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-matching-results-empty",
            )
    }

    test("매칭 결과 상세 조회 API 문서화") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)
        val matchingResult = MatchingResultFixture.create(
            id = matchingResultId,
            registerEmail = "holeman@naver.com",
            monthMatched = false,
            dayMatched = false,
        )

        every { matchingResultQueryService.findDetail(matchingResultId, "holeman@naver.com") } returns matchingResult

        // When & Then
        mockMvc.getJson("/api/matching-results/$encodedId") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-matching-result-detail",
                responseBody(
                    detailMatchingResultId(),
                    detailRemainingDays(),
                    detailMatchRate(),
                    middleNumberMatched(),
                    lastNumberMatched(),
                    yearMatched(),
                    monthMatched(),
                    dayMatched(),
                    regionMatched(),
                )
            )
    }
})
