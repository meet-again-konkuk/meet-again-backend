package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.application.MatchingResultCommandService
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.patchJson
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(MatchingResultCommandApi::class)
@BaseApiTest
class MatchingResultCommandApiTest(
    private val mockMvc: MockMvc,
    private val idObfuscator: IdObfuscator,
    @MockkBean private val matchingResultCommandService: MatchingResultCommandService,
) : FunSpec({

    test("매칭 결과 제외 API 문서화") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.exclude(matchingResultId, "holeman@naver.com") } just runs

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/exclude") {}
            .andExpect { status { isOk() } }
            .andDocument("matching/exclude-matching-result")
    }

    test("매칭 결과 제외 해제 API 문서화") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.include(matchingResultId, "holeman@naver.com") } just runs

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/include") {}
            .andExpect { status { isOk() } }
            .andDocument("matching/include-matching-result")
    }

    test("소유권이 없는 매칭 결과 제외 시 403을 반환한다") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.exclude(matchingResultId, "holeman@naver.com") } throws
            AccessDeniedException(EntityType.MATCHING_RESULT, "owner@example.com", "holeman@naver.com")

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/exclude") {}
            .andExpect { status { isForbidden() } }
    }
})
