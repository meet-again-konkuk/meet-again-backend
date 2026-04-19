package com.konkuk.ma.domain.point.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.konkuk.ma.config.JsonRedisTemplate
import com.konkuk.ma.domain.point.entity.CachedPointProductEntity
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class PointProductCacheDao(
    private val jsonRedisTemplate: JsonRedisTemplate,
) {
    companion object {
        private const val CACHE_KEY = "point-products:all"
        private const val CACHE_TTL_HOURS = 24L
    }

    fun findOrNull(): List<CachedPointProductEntity>? {
        return jsonRedisTemplate.get(CACHE_KEY, object : TypeReference<List<CachedPointProductEntity>>() {})
    }

    fun save(products: List<CachedPointProductEntity>) {
        jsonRedisTemplate.set(CACHE_KEY, products, CACHE_TTL_HOURS, TimeUnit.HOURS)
    }
}
