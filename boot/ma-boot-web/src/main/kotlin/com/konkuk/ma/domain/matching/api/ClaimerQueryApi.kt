package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.matching.api.response.ClaimersResponse
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/claimers")
class ClaimerQueryApi(
    private val matchingResultQueryService: MatchingResultQueryService,
) {
    @GetMapping("/me")
    fun findMyClaimers(
        @AuthenticationPrincipal email: String,
    ): ClaimersResponse {
        val profiles = matchingResultQueryService.findClaimedBy(email)
        return ClaimersResponse.from(profiles)
    }
}
