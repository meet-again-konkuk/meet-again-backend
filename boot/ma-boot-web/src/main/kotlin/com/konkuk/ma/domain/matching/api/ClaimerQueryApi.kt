package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.matching.api.response.ClaimersResponse
import com.konkuk.ma.domain.matching.application.MatchingResultQueryService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
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
        @LoginMember memberInfo: MemberInfo,
    ): ClaimersResponse {
        val profiles = matchingResultQueryService.findClaimedBy(memberInfo.email)
        return ClaimersResponse.from(profiles)
    }
}
