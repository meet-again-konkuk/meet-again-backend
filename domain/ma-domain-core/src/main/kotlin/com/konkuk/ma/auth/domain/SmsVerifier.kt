package com.konkuk.ma.auth.domain

import com.konkuk.ma.auth.domain.port.SmsRepository
import org.springframework.stereotype.Component

@Component
class SmsVerifier(
    private val smsRepository: SmsRepository
) {
    fun verify(phoneNumber: String, memberVerificationCode: Int): Boolean {
        val verificationCode = smsRepository.findOrNull(phoneNumber)
        val verified = memberVerificationCode == verificationCode
        if (verified) {
            smsRepository.confirmVerificationCode(phoneNumber)
        }
        return verified
    }
}
