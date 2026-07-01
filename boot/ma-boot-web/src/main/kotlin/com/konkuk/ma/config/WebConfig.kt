package com.konkuk.ma.config

import com.konkuk.ma.domain.common.domain.id.port.IdObfuscator
import com.konkuk.ma.support.id.DecryptIdConverter
import com.konkuk.ma.support.security.LoginMemberArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val idObfuscator: IdObfuscator,
    private val loginMemberArgumentResolver: LoginMemberArgumentResolver,
    @Value("\${file.upload.base-path:uploads}")
    private val basePath: String,
) : WebMvcConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(DecryptIdConverter(idObfuscator))
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginMemberArgumentResolver)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("$FILE_URL_PREFIX/**")
            .addResourceLocations("file:$basePath/")
    }

    companion object {
        const val FILE_URL_PREFIX = "/files"
    }
}
