package com.konkuk.ma.support.id

import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator

class EncryptIdAnnotationIntrospector(
    private val idObfuscator: IdObfuscator
) : NopAnnotationIntrospector() {

    // 외부 제약: Jackson의 findSerializer·findDeserializer는 컬렉션 전체에, findContentSerializer·findContentDeserializer는 원소에 적용된다.
    override fun findSerializer(a: Annotated): Any? {
        if (a.isContainer()) return null
        return createSerializerOrNull(a)
    }

    override fun findDeserializer(a: Annotated): Any? {
        if (a.isContainer()) return null
        return createDeserializerOrNull(a)
    }

    override fun findContentSerializer(a: Annotated): Any? = createSerializerOrNull(a)

    override fun findContentDeserializer(a: Annotated): Any? = createDeserializerOrNull(a)

    private fun Annotated.isContainer(): Boolean = type?.isContainerType == true

    private fun createSerializerOrNull(a: Annotated): EncryptIdSerializer? {
        val annotation = a.getAnnotation(EncryptId::class.java) ?: return null
        return EncryptIdSerializer(idObfuscator, annotation.value)
    }

    private fun createDeserializerOrNull(a: Annotated): EncryptIdDeserializer? {
        val annotation = a.getAnnotation(EncryptId::class.java) ?: return null
        return EncryptIdDeserializer(idObfuscator, annotation.value)
    }
}
