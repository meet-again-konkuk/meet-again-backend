package com.konkuk.ma.domain.auth.dao

import com.konkuk.ma.domain.auth.domain.VerificationCode
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class SmsVerificationFindDao(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    fun findOne(phoneNumber: String): VerificationCode? {
        return redisTemplate.opsForValue()
            .get(phoneNumber)
            ?.toString()
            ?.let { VerificationCode(it) }
    }

    fun getConfirmed(phoneNumber: String): Boolean {
        return redisTemplate.opsForValue()
            .get(phoneNumber)
            ?.toString()
            ?.toBoolean() ?: false
    }
}
