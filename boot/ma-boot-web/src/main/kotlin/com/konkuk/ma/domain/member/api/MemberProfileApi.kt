package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.member.api.request.UpdateMyProfileRequest
import com.konkuk.ma.domain.member.api.response.MyProfileResponse
import com.konkuk.ma.domain.member.application.MemberProfileCommandService
import com.konkuk.ma.domain.member.application.MemberProfileQueryService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members/me")
class MemberProfileApi(
    private val memberProfileQueryService: MemberProfileQueryService,
    private val memberProfileCommandService: MemberProfileCommandService,
) {
    @GetMapping
    fun findMyProfile(@LoginMember memberInfo: MemberInfo): MyProfileResponse {
        val profile = memberProfileQueryService.findOne(memberInfo.id)
        return MyProfileResponse.from(profile)
    }

    @PatchMapping
    fun updateMyProfile(
        @LoginMember memberInfo: MemberInfo,
        @Valid @RequestBody request: UpdateMyProfileRequest
    ): MyProfileResponse {
        val profile = memberProfileCommandService.update(memberInfo.id, request.toProfileChanges())
        return MyProfileResponse.from(profile)
    }
}
