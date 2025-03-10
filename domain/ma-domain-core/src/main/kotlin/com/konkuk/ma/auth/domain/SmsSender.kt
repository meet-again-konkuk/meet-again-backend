package com.konkuk.ma.auth.domain

import org.springframework.stereotype.Component

@Component
interface SmsSender {
    fun sendSmsVerificationCode(phoneNumber: String): SmsVerification
}
