package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.auth.application.WithdrawalCancelService
import com.konkuk.ma.domain.auth.application.WithdrawalService
import com.konkuk.ma.domain.member.api.request.WithdrawalCancelRequest
import com.konkuk.ma.domain.member.api.request.WithdrawalRequest
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members/withdrawal")
class MemberWithdrawalApi(
    private val withdrawalService: WithdrawalService,
    private val withdrawalCancelService: WithdrawalCancelService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun requestWithdrawal(
        @LoginMember memberInfo: MemberInfo,
        @Valid @RequestBody request: WithdrawalRequest
    ) {
        withdrawalService.requestWithdrawal(memberInfo.email, request.password)
    }

    @PostMapping("/cancellation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancelWithdrawal(
        @Valid @RequestBody request: WithdrawalCancelRequest
    ) {
        withdrawalCancelService.cancel(request.email, request.password)
    }
}
