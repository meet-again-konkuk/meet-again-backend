package com.konkuk.ma.support.id

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class EncryptIdDeserializer : JsonDeserializer<Long>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Long {
        return EncryptIdHolder.idObfuscator.decode(p.valueAsString)
    }
}
