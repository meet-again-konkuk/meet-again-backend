package com.konkuk.ma.domain.point.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.konkuk.ma.config.JsonRedisTemplate
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

    fun findOrNull(): List<CachedPointProduct>? {
        return jsonRedisTemplate.get(CACHE_KEY, object : TypeReference<List<CachedPointProduct>>() {})
    }

    fun save(products: List<CachedPointProduct>) {
        jsonRedisTemplate.set(CACHE_KEY, products, CACHE_TTL_HOURS, TimeUnit.HOURS)
    }
}

data class CachedPointProduct(
    val pointProductId: Long = 0,
    val name: String = "",
    val quantity: Int = 0,
    val price: Int = 0,
    val displayOrder: Int = 0,
    val discountType: String? = null,
    val discountAmount: Int? = null,
    val discountPercent: Int? = null,
    val discountStartDate: String? = null,
    val discountEndDate: String? = null,
)
