package com.konkuk.ma.domain.auth.api

import com.konkuk.ma.domain.auth.application.SmsVerificationService
import com.konkuk.ma.domain.auth.api.request.SmsSendRequest
import com.konkuk.ma.domain.auth.api.request.SmsVerificationConfirmRequest
import com.konkuk.ma.domain.auth.api.response.SmsVerificationConfirmResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sms")
class SmsVerificationApi(
    private val smsVerificationService: SmsVerificationService
) {
    @PostMapping("/verification-code")
    fun sendVerificationSms(@Valid @RequestBody request: SmsSendRequest) {
        smsVerificationService.sendSmsVerificationCode(request.phoneNumber)
    }

    @PostMapping("/verification-code/confirm")
    fun confirmVerificationSms(@Valid @RequestBody request: SmsVerificationConfirmRequest): SmsVerificationConfirmResponse {
        val verified = smsVerificationService.confirmVerificationCode(request.phoneNumber, request.verificationCode)
        return SmsVerificationConfirmResponse(request.phoneNumber, request.verificationCode, verified)
    }
}
