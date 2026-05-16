package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.member.api.request.WithdrawalRequest
import com.konkuk.ma.domain.member.api.response.WithdrawalCancelResponse
import com.konkuk.ma.domain.member.application.MemberWithdrawalCancelService
import com.konkuk.ma.domain.member.application.MemberWithdrawalService
import com.konkuk.ma.support.security.SkipWithdrawalGuard
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members/me/withdrawal")
class MemberWithdrawalApi(
    private val memberWithdrawalService: MemberWithdrawalService,
    private val memberWithdrawalCancelService: MemberWithdrawalCancelService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun requestWithdrawal(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: WithdrawalRequest
    ) {
        memberWithdrawalService.requestWithdrawal(request.toCommand(email))
    }

    @DeleteMapping
    @SkipWithdrawalGuard
    fun cancelWithdrawal(
        @AuthenticationPrincipal email: String
    ): WithdrawalCancelResponse {
        val result = memberWithdrawalCancelService.cancel(email)
        return WithdrawalCancelResponse.from(result)
    }
}
