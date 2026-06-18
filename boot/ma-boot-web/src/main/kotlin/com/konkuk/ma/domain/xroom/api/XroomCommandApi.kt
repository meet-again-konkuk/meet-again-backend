package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.response.CreateXroomResponse
import com.konkuk.ma.domain.xroom.application.XroomCommandService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/xrooms")
class XroomCommandApi(
    private val xroomCommandService: XroomCommandService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal memberInfo: MemberInfo,
        @RequestParam @DecryptId(ObfuscationType.TARGET_INFO) targetInfoId: Long,
    ): CreateXroomResponse {
        val xroomId = xroomCommandService.create(targetInfoId, memberInfo.email)
        return CreateXroomResponse(xroomId = xroomId)
    }
}
