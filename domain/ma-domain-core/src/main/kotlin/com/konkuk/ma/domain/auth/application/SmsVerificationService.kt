package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.SmsVerification
import com.konkuk.ma.domain.auth.domain.SmsVerifier
import com.konkuk.ma.domain.auth.domain.VerificationCode
import com.konkuk.ma.domain.auth.domain.port.SmsRepository
import com.konkuk.ma.domain.auth.domain.port.SmsSender
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
        val code = VerificationCode.random()
        smsSender.send(phoneNumber, code)
        smsRepository.save(SmsVerification(phoneNumber, code))
    }

    fun confirmVerificationCode(phoneNumber: String, memberVerificationCode: String): Boolean {
        return smsVerifier.verify(phoneNumber, memberVerificationCode)
    }
}
