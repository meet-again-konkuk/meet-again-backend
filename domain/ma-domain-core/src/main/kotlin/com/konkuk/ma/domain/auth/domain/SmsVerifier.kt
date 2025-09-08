package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import org.springframework.stereotype.Component

@Component
class SmsVerifier(
    private val smsRepository: SmsRepository
) {
    fun verify(phoneNumber: String, inputVerificationCode: Int): Boolean {
        val verificationCode = smsRepository.findOrNull(phoneNumber)
        val verified = inputVerificationCode == verificationCode
        if (verified) {
            smsRepository.confirmVerificationCode(phoneNumber)
        }
        return verified
    }
}
