package com.konkuk.ma.domain.point.dao

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
        private const val CACHE_TTL_DAYS = 7L
    }

    fun findOrNull(): List<CachedPointProductEntity>? {
        return jsonRedisTemplate.hvalsOrNull(CACHE_KEY, CachedPointProductEntity::class.java)
    }

    fun findOneOrNull(pointProductId: Long): CachedPointProductEntity? {
        return jsonRedisTemplate.hget(CACHE_KEY, pointProductId.toString(), CachedPointProductEntity::class.java)
    }

    fun save(products: List<CachedPointProductEntity>) {
        val fields = products.associate { it.pointProductId.toString() to it }
        jsonRedisTemplate.replaceHash(CACHE_KEY, fields, CACHE_TTL_DAYS, TimeUnit.DAYS)
    }
}
