package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.member.api.request.DuplicatedNicknameRequest
import com.konkuk.ma.domain.member.api.response.CheckDuplicatedNicknameResponse
import com.konkuk.ma.member.application.MemberQueryService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberQueryApi(
    private val memberQueryService: MemberQueryService
) {
    @GetMapping("/duplicated-nickname")
    fun checkDuplicatedNickname(@Validated request: DuplicatedNicknameRequest): CheckDuplicatedNicknameResponse {
        val duplicated = memberQueryService.checkDuplicatedNickname(request.nickname)
        return CheckDuplicatedNicknameResponse(
            nickname = request.nickname,
            duplicated = duplicated
        )
    }
}
