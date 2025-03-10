package com.konkuk.ma.auth.application

import com.konkuk.ma.auth.domain.SmsRedisRepository
import com.konkuk.ma.auth.domain.SmsSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SmsVerificationService(
    private val smsSender: SmsSender,

    private val smsRedisRepository: SmsRedisRepository
) {
    fun sendSmsVerificationCode(phoneNumber: String) {
        val smsVerification = smsSender.sendSmsVerificationCode(phoneNumber)
        smsRedisRepository.save(smsVerification)
    }

    fun confirmVerificationCode(phoneNumber: String, confirmVerificationCode: Int): Boolean {
        val verificationCode = smsRedisRepository.find(phoneNumber)
        return verificationCode == confirmVerificationCode
    }
}
