package com.konkuk.ma.domain.auth.dao

import com.konkuk.ma.domain.auth.domain.SmsVerification
import java.util.concurrent.TimeUnit
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class SmsVerificationSaveDao(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    companion object {
        const val SMS_VERIFICATION_EXPIRE_MINUTE = 3L
        const val CONFIRM_VERIFICATION_EXPIRE_MINUTE = 10L
    }

    fun save(smsVerification: SmsVerification) {
        redisTemplate.opsForValue().set(smsVerification.phoneNumber, smsVerification.verificationCode.value, SMS_VERIFICATION_EXPIRE_MINUTE, TimeUnit.MINUTES)
    }

    fun save(phoneNumber: String) {
        redisTemplate.opsForValue().set(phoneNumber, true.toString(), CONFIRM_VERIFICATION_EXPIRE_MINUTE, TimeUnit.MINUTES)
    }
}
