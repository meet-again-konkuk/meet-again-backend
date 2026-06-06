package com.konkuk.ma.domain.member.api

import com.konkuk.ma.domain.auth.application.WithdrawalCancelService
import com.konkuk.ma.domain.auth.application.WithdrawalService
import com.konkuk.ma.domain.member.api.request.WithdrawalRequest
import com.konkuk.ma.domain.member.api.response.WithdrawalCancelResponse
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
    private val withdrawalService: WithdrawalService,
    private val withdrawalCancelService: WithdrawalCancelService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun requestWithdrawal(
        @AuthenticationPrincipal email: String,
        @Valid @RequestBody request: WithdrawalRequest
    ) {
        withdrawalService.requestWithdrawal(request.toCommand(email))
    }

    @DeleteMapping
    fun cancelWithdrawal(
        @AuthenticationPrincipal email: String
    ): WithdrawalCancelResponse {
        val result = withdrawalCancelService.cancel(email)
        return WithdrawalCancelResponse.from(result)
    }
}
