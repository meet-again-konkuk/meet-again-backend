package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.exception.InvalidObfuscatedIdException
import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import org.hashids.Hashids
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class HashidsIdObfuscator(
    @Value("\${id-obfuscator.salt}") private val baseSalt: String,
    @Value("\${id-obfuscator.min-length:12}") private val minLength: Int
) : IdObfuscator {

    private val cache = ConcurrentHashMap<ObfuscationType, Hashids>()

    override fun encode(type: ObfuscationType, id: Long): String {
        return hashidsFor(type).encode(id)
    }

    override fun decode(type: ObfuscationType, encoded: String): Long {
        val decoded = hashidsFor(type).decode(encoded)
        if (decoded.isEmpty()) {
            throw InvalidObfuscatedIdException(encoded)
        }
        return decoded[0]
    }

    private fun hashidsFor(type: ObfuscationType): Hashids {
        return cache.computeIfAbsent(type) {
            Hashids("$baseSalt:${type.saltSuffix}", minLength)
        }
    }
}
