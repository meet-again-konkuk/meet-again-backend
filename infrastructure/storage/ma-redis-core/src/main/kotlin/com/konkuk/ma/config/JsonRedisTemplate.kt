package com.konkuk.ma.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class JsonRedisTemplate(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {
    fun <T> get(key: String, typeRef: TypeReference<T>): T? {
        val json = redisTemplate.opsForValue().get(key)?.toString() ?: return null
        return objectMapper.readValue(json, typeRef)
    }

    fun set(key: String, value: Any, ttl: Long, unit: TimeUnit) {
        val json = objectMapper.writeValueAsString(value)
        redisTemplate.opsForValue().set(key, json, ttl, unit)
    }
}
