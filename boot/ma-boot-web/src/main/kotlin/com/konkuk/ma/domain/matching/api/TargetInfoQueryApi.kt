package com.konkuk.ma.domain.matching.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.matching.api.response.TargetInfoResponse
import com.konkuk.ma.domain.matching.application.TargetInfoQueryService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
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
        @LoginMember memberInfo: MemberInfo,
    ): List<TargetInfoResponse> {
        return targetInfoQueryService.find(memberInfo.id)
            .map { TargetInfoResponse.from(it) }
    }

    @GetMapping("/{targetInfoId}")
    fun findTargetInfoDetail(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long,
    ): TargetInfoResponse {
        val targetInfo = targetInfoQueryService.findDetail(targetInfoId, memberInfo.id)
        return TargetInfoResponse.from(targetInfo)
    }
}
