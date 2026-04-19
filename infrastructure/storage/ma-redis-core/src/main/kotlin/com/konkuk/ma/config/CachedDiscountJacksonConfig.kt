package com.konkuk.ma.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.konkuk.ma.domain.point.entity.CachedAmountDiscount
import com.konkuk.ma.domain.point.entity.CachedDiscount
import com.konkuk.ma.domain.point.entity.CachedPercentDiscount
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CachedDiscountJacksonConfig {

    @Bean
    fun cachedDiscountMixinCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.mixIn(CachedDiscount::class.java, CachedDiscountMixin::class.java)
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = CachedAmountDiscount::class, name = "AMOUNT"),
        JsonSubTypes.Type(value = CachedPercentDiscount::class, name = "PERCENT"),
    )
    private interface CachedDiscountMixin
}
