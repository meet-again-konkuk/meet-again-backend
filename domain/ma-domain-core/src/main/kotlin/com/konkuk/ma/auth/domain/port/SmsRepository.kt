package com.konkuk.ma.auth.domain.port

import com.konkuk.ma.auth.domain.SmsVerification

interface SmsRepository {
    fun save(smsVerification: SmsVerification)

    fun findOrNull(phoneNumber: String): Int?

    fun confirmVerificationCode(phoneNumber: String)

    fun getConfirmed(phoneNumber: String): Boolean
}
