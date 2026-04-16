package com.konkuk.ma.domain.point.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PointProductCacheDao(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val CACHE_KEY = "point-products:all"
        private const val CACHE_TTL_HOURS = 24L
    }

    fun findOrNull(): List<CachedPointProduct>? {
        val json = redisTemplate.opsForValue().get(CACHE_KEY)?.toString() ?: return null
        return objectMapper.readValue(json, object : TypeReference<List<CachedPointProduct>>() {})
    }

    fun save(products: List<CachedPointProduct>) {
        val json = objectMapper.writeValueAsString(products)
        redisTemplate.opsForValue().set(CACHE_KEY, json, CACHE_TTL_HOURS, TimeUnit.HOURS)
    }
}

data class CachedPointProduct(
    val pointProductId: Long = 0,
    val name: String = "",
    val quantity: Int = 0,
    val price: Int = 0,
    val displayOrder: Int = 0,
)
