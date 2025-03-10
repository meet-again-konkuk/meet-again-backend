package com.konkuk.ma.config

import net.nurigo.sdk.NurigoApp.initialize
import net.nurigo.sdk.message.service.DefaultMessageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoolSmsConfig(
    @Value("\${cool-sms.api-key}")
    private val apiKey: String,

    @Value("\${cool-sms.api-secret}")
    private val apiSecret: String
) {
    @Bean
    fun messageService(): DefaultMessageService {
        return initialize(apiKey, apiSecret, "https://api.coolsms.co.kr")
    }
}
