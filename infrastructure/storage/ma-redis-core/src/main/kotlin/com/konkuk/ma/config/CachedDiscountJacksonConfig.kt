package com.konkuk.ma.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.konkuk.ma.domain.point.entity.CachedDiscount
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.reflect.full.createInstance

@Configuration
class CachedDiscountJacksonConfig {

    @Bean
    fun cachedDiscountJacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.mixIn(CachedDiscount::class.java, CachedDiscountTypeInfoMixin::class.java)
            builder.postConfigurer { mapper ->
                CachedDiscount::class.sealedSubclasses.forEach { subclass ->
                    val instance = subclass.createInstance()
                    mapper.registerSubtypes(NamedType(subclass.java, instance.type.name))
                }
            }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
    private interface CachedDiscountTypeInfoMixin
}
