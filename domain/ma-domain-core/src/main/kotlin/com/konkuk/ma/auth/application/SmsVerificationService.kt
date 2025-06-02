package com.konkuk.ma.auth.application

import com.konkuk.ma.auth.domain.SmsVerifier
import com.konkuk.ma.auth.domain.port.SmsRepository
import com.konkuk.ma.auth.domain.port.SmsSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SmsVerificationService(
    private val smsSender: SmsSender,

    private val smsRepository: SmsRepository,

    private val smsVerifier: SmsVerifier
) {
    fun sendSmsVerificationCode(phoneNumber: String) {
        val smsVerification = smsSender.sendSmsVerificationCode(phoneNumber)
        smsRepository.save(smsVerification)
    }

    fun confirmVerificationCode(phoneNumber: String, memberVerificationCode: Int): Boolean {
        return smsVerifier.verify(phoneNumber, memberVerificationCode)
    }
}
