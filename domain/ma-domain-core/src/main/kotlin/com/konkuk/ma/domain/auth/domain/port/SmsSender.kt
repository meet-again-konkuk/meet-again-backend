package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.SmsVerification
import org.springframework.stereotype.Component

@Component
interface SmsSender {
    fun sendSmsVerificationCode(phoneNumber: String): SmsVerification
}
