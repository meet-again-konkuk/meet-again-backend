package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import com.konkuk.ma.domain.common.port.IdObfuscator
import org.hashids.Hashids
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class HashidsIdObfuscator(
    @Value("\${id-obfuscator.salt}") salt: String,
    @Value("\${id-obfuscator.min-length:8}") minLength: Int
) : IdObfuscator {

    private val hashids = Hashids(salt, minLength)

    override fun encode(id: Long): String {
        return hashids.encode(id)
    }

    override fun decode(encoded: String): Long {
        val decoded = hashids.decode(encoded)
        if (decoded.isEmpty()) {
            throw InvalidObfuscatedIdException(encoded)
        }
        return decoded[0]
    }
}
