package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.common.domain.id.ObfuscationType

class EncryptIdDeserializer(
    private val idObfuscator: IdObfuscator,
    private val type: ObfuscationType
) : JsonDeserializer<Long>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Long {
        return idObfuscator.decode(type, p.valueAsString)
    }
}
