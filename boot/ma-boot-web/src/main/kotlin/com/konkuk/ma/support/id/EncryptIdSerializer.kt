package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.common.domain.id.ObfuscationType

class EncryptIdSerializer(
    private val idObfuscator: IdObfuscator,
    private val type: ObfuscationType
) : JsonSerializer<Long>() {

    override fun serialize(value: Long, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(idObfuscator.encode(type, value))
    }
}
