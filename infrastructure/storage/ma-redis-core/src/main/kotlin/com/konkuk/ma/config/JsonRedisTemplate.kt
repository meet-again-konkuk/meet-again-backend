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

    fun <T> hget(key: String, field: String, clazz: Class<T>): T? {
        val raw = redisTemplate.opsForHash<String, Any>().get(key, field)?.toString() ?: return null
        return objectMapper.readValue(raw, clazz)
    }

    fun <T> hvalsOrNull(key: String, clazz: Class<T>): List<T>? {
        if (redisTemplate.hasKey(key) != true) return null
        return redisTemplate.opsForHash<String, Any>().values(key)
            .map { objectMapper.readValue(it.toString(), clazz) }
    }

    fun replaceHash(key: String, fields: Map<String, Any>, ttl: Long, unit: TimeUnit) {
        redisTemplate.delete(key)
        if (fields.isEmpty()) return
        val jsonMap = fields.mapValues { objectMapper.writeValueAsString(it.value) }
        redisTemplate.opsForHash<String, Any>().putAll(key, jsonMap)
        redisTemplate.expire(key, ttl, unit)
    }
}
