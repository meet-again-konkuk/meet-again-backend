package com.konkuk.ma.auth.domain.port

import com.konkuk.ma.auth.domain.SmsVerification
import org.springframework.stereotype.Component

@Component
interface SmsSender {
    fun sendSmsVerificationCode(phoneNumber: String): SmsVerification
}
