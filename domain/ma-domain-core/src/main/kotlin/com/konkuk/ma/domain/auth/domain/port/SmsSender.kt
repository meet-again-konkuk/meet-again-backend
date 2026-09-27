package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.VerificationCode
import org.springframework.stereotype.Component

@Component
interface SmsSender {
    fun send(phoneNumber: String, code: VerificationCode)
}
