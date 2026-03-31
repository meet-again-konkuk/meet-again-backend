package com.konkuk.ma.support.id

import com.konkuk.ma.domain.common.port.IdObfuscator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestIdObfuscatorConfig {

    @Bean
    fun idObfuscator(): IdObfuscator {
        val obfuscator = HashidsIdObfuscator(
            salt = "test-salt",
            minLength = 8
        )
        EncryptIdHolder.idObfuscator = obfuscator
        return obfuscator
    }
}
