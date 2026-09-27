package com.konkuk.ma.domain.xroom.api

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.response.MyXroomsResponse
import com.konkuk.ma.domain.xroom.api.response.ReceivedXroomsResponse
import com.konkuk.ma.domain.xroom.api.response.XroomDetailResponse
import com.konkuk.ma.domain.xroom.application.XroomQueryService
import com.konkuk.ma.support.id.DecryptId
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/xrooms")
class XroomQueryApi(
    private val xroomQueryService: XroomQueryService,
) {
    @GetMapping("/me")
    fun findMine(
        @LoginMember memberInfo: MemberInfo,
    ): MyXroomsResponse {
        val myXrooms = xroomQueryService.findMine(memberInfo.id)
        return MyXroomsResponse.from(myXrooms)
    }

    @GetMapping("/received")
    fun findReceived(
        @LoginMember memberInfo: MemberInfo,
    ): ReceivedXroomsResponse {
        val receivedXrooms = xroomQueryService.findReceived(memberInfo.id)
        return ReceivedXroomsResponse.from(receivedXrooms)
    }

    @GetMapping("/{xroomId}")
    fun findDetail(
        @PathVariable @DecryptId(ObfuscationType.XROOM) xroomId: Long,
        @LoginMember memberInfo: MemberInfo,
    ): XroomDetailResponse {
        val xroomDetail = xroomQueryService.findDetail(xroomId, memberInfo.id)
        return XroomDetailResponse.from(xroomDetail)
    }
}
