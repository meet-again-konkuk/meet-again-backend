package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.api.response.TargetInfoResponse
import com.konkuk.ma.domain.matching.application.TargetInfoQueryService
import com.konkuk.ma.support.id.DecryptId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/target-infos")
class TargetInfoQueryApi(
    private val targetInfoQueryService: TargetInfoQueryService,
) {
    @GetMapping
    fun findMyTargetInfos(
        @AuthenticationPrincipal email: String,
    ): List<TargetInfoResponse> {
        return targetInfoQueryService.find(Email(email))
            .map { TargetInfoResponse.from(it) }
    }

    @GetMapping("/{targetInfoId}")
    fun findTargetInfoDetail(
        @AuthenticationPrincipal email: String,
        @PathVariable @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long,
    ): TargetInfoResponse {
        val targetInfo = targetInfoQueryService.findDetail(targetInfoId, Email(email))
        return TargetInfoResponse.from(targetInfo)
    }
}
