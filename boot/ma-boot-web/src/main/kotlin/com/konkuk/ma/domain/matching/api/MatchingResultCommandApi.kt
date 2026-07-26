package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.application.MatchingResultCommandService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
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
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.exclude(matchingResultId, memberInfo.id)
    }

    @PatchMapping("/{matchingResultId}/include")
    fun include(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.include(matchingResultId, memberInfo.id)
    }

    @PatchMapping("/{matchingResultId}/claim")
    fun claim(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.claim(matchingResultId, memberInfo.id)
    }

    @PatchMapping("/{matchingResultId}/reject")
    fun reject(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.MATCHING_RESULT) matchingResultId: Long,
    ) {
        matchingResultCommandService.reject(matchingResultId, memberInfo.id)
    }
}
