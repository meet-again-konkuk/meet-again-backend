package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.application.MatchingResultCommandService
import com.konkuk.ma.support.id.DecryptId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/matching-results")
class MatchingResultCommandApi(
    private val matchingResultCommandService: MatchingResultCommandService,
) {
    @PatchMapping("/{matchingResultId}/exclude")
    fun exclude(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.exclude(matchingResultId, email)
    }

    @PatchMapping("/{matchingResultId}/include")
    fun include(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.include(matchingResultId, email)
    }
}
