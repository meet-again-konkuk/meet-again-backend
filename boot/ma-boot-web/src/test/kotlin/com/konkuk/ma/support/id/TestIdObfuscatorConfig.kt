package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestIdObfuscatorConfig {

    @Bean
    @Primary
    fun testIdObfuscator(): IdObfuscator {
        return HashidsIdObfuscator(
            baseSalt = "test-salt",
            minLength = 12
        )
    }

    @Bean
    fun testEncryptIdIntrospectorInit(
        objectMapper: ObjectMapper,
        idObfuscator: IdObfuscator
    ): EncryptIdAnnotationIntrospector {
        val introspector = EncryptIdAnnotationIntrospector(idObfuscator)
        val existing = objectMapper.serializationConfig.annotationIntrospector
        objectMapper.setAnnotationIntrospector(
            AnnotationIntrospector.pair(introspector, existing)
        )
        return introspector
    }
}
