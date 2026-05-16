package com.konkuk.ma.config

import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.support.id.DecryptIdConverter
import com.konkuk.ma.support.security.WithdrawalGuardInterceptor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val idObfuscator: IdObfuscator,
    private val withdrawalGuardInterceptorProvider: ObjectProvider<WithdrawalGuardInterceptor>
) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(DecryptIdConverter(idObfuscator))
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        withdrawalGuardInterceptorProvider.ifAvailable { registry.addInterceptor(it) }
    }
}
