package com.konkuk.ma.config

import com.konkuk.ma.domain.member.application.MemberQueryService
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MemberQueryServiceTestConfig {
    @Bean
    fun memberQueryService(): MemberQueryService = mockk(relaxed = true)
}
