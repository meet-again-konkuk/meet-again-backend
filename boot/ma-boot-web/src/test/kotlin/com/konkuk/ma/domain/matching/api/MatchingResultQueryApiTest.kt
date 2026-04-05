package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.MatchingResultWithProfile
import com.konkuk.ma.domain.matching.domain.MatchingResultsWithProfiles
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.matchRate
import com.konkuk.ma.vocabulary.matchingResultId
import com.konkuk.ma.vocabulary.matchingTargetName
import com.konkuk.ma.vocabulary.matchingTargetNickname
import com.konkuk.ma.vocabulary.profileImageUrl
import com.konkuk.ma.vocabulary.remainingDays
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(MatchingResultQueryApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class MatchingResultQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val matchingResultQueryService: MatchingResultQueryService
) : FunSpec({

    test("매칭 결과 목록 조회 API 문서화") {
        // Given
        val matchingResult = MatchingResult(
            id = 1L,
            registerEmail = "test@example.com",
            targetInfoId = 10L,
            targetEmail = "target@example.com",
            middleNumberMatched = true,
            lastNumberMatched = true,
            yearMatched = true,
            monthMatched = false,
            dayMatched = false,
            regionMatched = true,
            showingExpiryDate = LocalDateTime.now().plusDays(25),
            matchingExpiryDate = LocalDate.now().plusDays(200),
        )
        val resultsWithProfiles = MatchingResultsWithProfiles(
            data = listOf(
                MatchingResultWithProfile(
                    matchingResult = matchingResult,
                    targetName = "김만남",
                    targetNickname = "테스트닉네임",
                    profileImageUrl = "https://example.com/image.jpg",
                )
            )
        )

        every { matchingResultQueryService.findByRegisterEmail("test@example.com") } returns resultsWithProfiles

        // When & Then
        mockMvc.getJson("/api/matching-results") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-matching-results",
                responseBody(
                    matchingResultId(),
                    matchingTargetName(),
                    matchingTargetNickname(),
                    profileImageUrl(),
                    remainingDays(),
                    matchRate(),
                )
            )
    }

    test("매칭 결과가 없는 경우 빈 목록 반환") {
        // Given
        val emptyResults = MatchingResultsWithProfiles(data = emptyList())

        every { matchingResultQueryService.findByRegisterEmail("test@example.com") } returns emptyResults

        // When & Then
        mockMvc.getJson("/api/matching-results") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/find-matching-results-empty",
            )
    }
})
