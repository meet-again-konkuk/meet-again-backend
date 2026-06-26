package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.request.CreateXroomRequest
import com.konkuk.ma.domain.xroom.api.request.UpdateFinalMessageRequest
import com.konkuk.ma.domain.xroom.api.response.XroomResponse
import com.konkuk.ma.domain.xroom.application.XroomCommandService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
        @LoginMember memberInfo: MemberInfo,
        @RequestBody request: CreateXroomRequest,
    ): XroomResponse {
        val xroomId = xroomCommandService.create(request.targetInfoId, memberInfo.id, request.finalMessage)
        return XroomResponse(xroomId = xroomId)
    }

    @PatchMapping("/{xroomId}")
    fun updateFinalMessage(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @RequestBody request: UpdateFinalMessageRequest,
    ): XroomResponse {
        val updatedId = xroomCommandService.updateFinalMessage(xroomId, memberInfo.id, request.finalMessage)
        return XroomResponse(xroomId = updatedId)
    }
}
