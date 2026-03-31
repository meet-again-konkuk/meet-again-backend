package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator

class EncryptIdAnnotationIntrospector(
    private val idObfuscator: IdObfuscator
) : NopAnnotationIntrospector() {

    override fun findSerializer(a: Annotated): Any? {
        val annotation = a.getAnnotation(EncryptId::class.java) ?: return null
        return EncryptIdSerializer(idObfuscator, annotation.value)
    }

    override fun findDeserializer(a: Annotated): Any? {
        val annotation = a.getAnnotation(EncryptId::class.java) ?: return null
        return EncryptIdDeserializer(idObfuscator, annotation.value)
    }
}
