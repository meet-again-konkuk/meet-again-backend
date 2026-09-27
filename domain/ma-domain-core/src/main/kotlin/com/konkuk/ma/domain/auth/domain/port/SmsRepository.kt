package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.SmsVerification
import com.konkuk.ma.domain.auth.domain.VerificationCode

interface SmsRepository {
    fun save(smsVerification: SmsVerification)

    fun findOrNull(phoneNumber: String): VerificationCode?

    fun confirmVerificationCode(phoneNumber: String)

    fun getConfirmed(phoneNumber: String): Boolean
}
