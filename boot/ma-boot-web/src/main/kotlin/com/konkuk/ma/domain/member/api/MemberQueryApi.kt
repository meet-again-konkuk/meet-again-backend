package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.member.api.request.DuplicatedEmailRequest
import com.konkuk.ma.domain.member.api.request.DuplicatedNicknameRequest
import com.konkuk.ma.domain.member.api.response.CheckDuplicatedEmailResponse
import com.konkuk.ma.domain.member.api.response.CheckDuplicatedNicknameResponse
import com.konkuk.ma.domain.member.application.MemberQueryService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberQueryApi(
    private val memberQueryService: MemberQueryService
) {
    @PostMapping("/duplicated-nickname")
    fun checkDuplicatedNickname(@Validated @RequestBody request: DuplicatedNicknameRequest): CheckDuplicatedNicknameResponse {
        val duplicated = memberQueryService.checkDuplicatedNickname(request.nickname)
        return CheckDuplicatedNicknameResponse(
            nickname = request.nickname,
            duplicated = duplicated
        )
    }

    @PostMapping("/duplicated-email")
    fun checkDuplicatedEmail(@Validated @RequestBody request: DuplicatedEmailRequest): CheckDuplicatedEmailResponse {
        val duplicated = memberQueryService.checkDuplicatedEmail(request.email)
        return CheckDuplicatedEmailResponse(
            email = request.email,
            duplicated = duplicated
        )
    }

}
