package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class ObfuscatedIdJacksonConfig(
    private val objectMapper: ObjectMapper,
    private val idObfuscator: IdObfuscator
) {
    @PostConstruct
    fun registerEncryptIdIntrospector() {
        val introspector = EncryptIdAnnotationIntrospector(idObfuscator)
        val existing = objectMapper.serializationConfig.annotationIntrospector
        objectMapper.setAnnotationIntrospector(
            AnnotationIntrospector.pair(introspector, existing)
        )
    }
}
