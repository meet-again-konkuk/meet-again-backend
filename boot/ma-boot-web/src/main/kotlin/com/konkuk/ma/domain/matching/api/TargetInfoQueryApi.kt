package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.matching.api.response.TargetInfoResponse
import com.konkuk.ma.domain.matching.application.TargetInfoQueryService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
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
        return targetInfoQueryService.find(email)
            .map { TargetInfoResponse.from(it) }
    }
}
