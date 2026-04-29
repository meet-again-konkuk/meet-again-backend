package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import org.springframework.stereotype.Component

@Component
class SmsVerifier(
    private val smsRepository: SmsRepository
) {
    fun verify(phoneNumber: String, inputVerificationCode: String): Boolean {
        val storedCode = smsRepository.findOrNull(phoneNumber)
        val verified = storedCode?.value == inputVerificationCode
        if (verified) {
            smsRepository.confirmVerificationCode(phoneNumber)
        }
        return verified
    }
}
