package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.api.response.MatchingResultDetailResponse
import com.konkuk.ma.domain.matching.api.response.MatchingResultsResponse
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.support.id.DecryptId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching-results")
class MatchingResultQueryApi(
    private val matchingResultQueryService: MatchingResultQueryService
) {
    @GetMapping
    fun findMyMatchingResults(
        @AuthenticationPrincipal email: String,
        @RequestParam(defaultValue = "false") excluded: Boolean,
    ): MatchingResultsResponse {
        val results = matchingResultQueryService.find(email, excluded)
        return MatchingResultsResponse.from(results)
    }

    @GetMapping("/{matchingResultId}")
    fun findMatchingResultDetail(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ): MatchingResultDetailResponse {
        val matchingResult = matchingResultQueryService.findDetailById(matchingResultId, email)
        return MatchingResultDetailResponse.from(matchingResult)
    }
}
