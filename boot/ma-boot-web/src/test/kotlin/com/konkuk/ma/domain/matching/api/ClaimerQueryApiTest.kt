package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.domain.matching.domain.ClaimerProfile
import com.konkuk.ma.domain.matching.domain.ClaimerProfiles
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.hasXroom
import com.konkuk.ma.vocabulary.isWithdrawn
import com.konkuk.ma.vocabulary.memberId
import com.konkuk.ma.vocabulary.name
import com.konkuk.ma.vocabulary.nickname
import com.konkuk.ma.vocabulary.profileImageUrl
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(ClaimerQueryApi::class)
@BaseApiTest
class ClaimerQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val matchingResultQueryService: MatchingResultQueryService,
) : FunSpec({

    test("나를 claim한 요청자 목록 조회 API 문서화") {
        val profiles = ClaimerProfiles(
            data = listOf(
                ClaimerProfile(
                    memberId = 1L,
                    name = "김클레임",
                    nickname = "클레이머닉네임",
                    profileImageUrl = "https://example.com/claimer.jpg",
                    hasXroom = true,
                ),
                ClaimerProfile(
                    memberId = 2L,
                    name = "이신청",
                    nickname = "신청자닉네임",
                    profileImageUrl = null,
                    hasXroom = false,
                ),
            )
        )

        every { matchingResultQueryService.findClaimedBy(1L) } returns profiles

        mockMvc.getJson("/api/claimers/me") {}
            .andExpect { status { isOk() } }
            .andDocument(
                "claimer/find-my-claimers",
                responseBody(
                    memberId("claimers[].memberId"),
                    name("claimers[].name"),
                    nickname("claimers[].nickname"),
                    profileImageUrl("claimers[].profileImageUrl"),
                    isWithdrawn("claimers[].isWithdrawn"),
                    hasXroom(),
                ),
            )
    }
})
