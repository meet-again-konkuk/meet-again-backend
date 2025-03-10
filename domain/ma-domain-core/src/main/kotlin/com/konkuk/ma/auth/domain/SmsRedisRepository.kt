package com.konkuk.ma.auth.domain

interface SmsRedisRepository {
    fun save(smsVerification: SmsVerification)

    fun find(phoneNumber: String): Int?
}
