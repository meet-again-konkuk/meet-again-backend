package com.konkuk.ma.domain.auth.dao

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class SmsVerificationFindDao(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    fun find(phoneNumber: String): Int? {
        return redisTemplate.opsForValue()
            .get(phoneNumber)
            ?.toString()
            ?.toInt()
    }
}
