package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class ObfuscatedIdJacksonConfig(
    private val idObfuscator: IdObfuscator
) {
    @PostConstruct
    fun initializeEncryptIdHolder() {
        EncryptIdHolder.idObfuscator = idObfuscator
    }
}
