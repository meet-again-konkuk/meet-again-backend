package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.matching.api.response.MatchingResultsResponse
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching-results")
class MatchingResultQueryApi(
    private val matchingResultQueryService: MatchingResultQueryService
) {
    @GetMapping
    fun findMyMatchingResults(
        @AuthenticationPrincipal email: String,
    ): MatchingResultsResponse {
        val results = matchingResultQueryService.findByRegisterEmail(email)
        return MatchingResultsResponse.from(results)
    }
}
