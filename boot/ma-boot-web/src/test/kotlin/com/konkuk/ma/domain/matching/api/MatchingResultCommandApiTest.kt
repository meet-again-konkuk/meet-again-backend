package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.config.BaseApiTest

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.matching.application.MatchingResultCommandService
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.vocabulary.matchingResultIdPath
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

        every { matchingResultCommandService.exclude(matchingResultId, 1L) } just runs

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/exclude") {}
            .andExpect { status { isOk() } }
            .andDocument("matching/exclude-matching-result")
    }

    test("매칭 결과 제외 해제 API 문서화") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.include(matchingResultId, 1L) } just runs

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/include") {}
            .andExpect { status { isOk() } }
            .andDocument("matching/include-matching-result")
    }

    test("매칭 결과 claim API 문서화") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.claim(matchingResultId, 1L) } just runs

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/claim") {}
            .andExpect { status { isOk() } }
            .andDocument("matching/claim-matching-result")
    }

    test("매칭 결과 claim 거절 API 문서화") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.reject(matchingResultId, 1L) } just runs

        // When & Then
        mockMvc.patchJson("/api/matching-results/{matchingResultId}/reject", encodedId)
            .andExpect { status { isOk() } }
            .andDocument(
                "matching/reject-matching-result",
                pathVariables(
                    matchingResultIdPath(),
                ),
            )
    }

    test("소유권이 없는 매칭 결과 제외 시 403을 반환한다") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.exclude(matchingResultId, 1L) } throws
            AccessDeniedException(EntityType.MATCHING_RESULT, "2", "1")

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/exclude") {}
            .andExpect { status { isForbidden() } }
    }

    test("수신자가 아닌 회원이 claim을 거절하면 403을 반환한다") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.reject(matchingResultId, 1L) } throws
            AccessDeniedException(EntityType.MATCHING_RESULT, "2", "1")

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/reject") {}
            .andExpect { status { isForbidden() } }
    }

    test("존재하지 않는 매칭 결과를 거절하면 404를 반환한다") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.reject(matchingResultId, 1L) } throws
            EntityNotFoundException(EntityType.MATCHING_RESULT, "1")

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/reject") {}
            .andExpect { status { isNotFound() } }
    }

    test("CLAIMED 상태가 아닌 매칭 결과를 거절하면 400을 반환한다") {
        // Given
        val matchingResultId = 1L
        val encodedId = idObfuscator.encode(ObfuscationType.MATCHING_RESULT, matchingResultId)

        every { matchingResultCommandService.reject(matchingResultId, 1L) } throws
            InvalidStateException(
                MatchingResultCommandApiTest::class,
                matchingResultId,
                "claim 상태가 아니어서 거절할 수 없습니다.",
            )

        // When & Then
        mockMvc.patchJson("/api/matching-results/$encodedId/reject") {}
            .andExpect { status { isBadRequest() } }
    }
})
