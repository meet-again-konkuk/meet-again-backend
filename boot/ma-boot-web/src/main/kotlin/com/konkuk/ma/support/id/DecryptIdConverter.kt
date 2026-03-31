package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.ConditionalGenericConverter
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair

class DecryptIdConverter(
    private val idObfuscator: IdObfuscator
) : ConditionalGenericConverter {

    override fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean {
        return targetType.hasAnnotation(DecryptId::class.java)
    }

    override fun getConvertibleTypes(): Set<ConvertiblePair> {
        return setOf(
            ConvertiblePair(String::class.java, Long::class.java),
            ConvertiblePair(String::class.java, Long::class.javaObjectType)
        )
    }

    override fun convert(source: Any?, sourceType: TypeDescriptor, targetType: TypeDescriptor): Any? {
        if (source == null) return null
        val annotation = targetType.getAnnotation(DecryptId::class.java)
            ?: throw IllegalStateException("@DecryptId 어노테이션을 찾을 수 없습니다.")
        return idObfuscator.decode(annotation.value, source as String)
    }
}
