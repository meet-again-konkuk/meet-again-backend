package com.konkuk.ma.config

import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.support.id.DecryptIdConverter
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val idObfuscator: IdObfuscator
) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(DecryptIdConverter(idObfuscator))
    }
}
