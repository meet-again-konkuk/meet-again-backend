package com.konkuk.ma.config

import com.konkuk.ma.auth.JwtManager
import com.konkuk.ma.support.id.TestIdObfuscatorConfig
import com.konkuk.ma.support.security.RoutingAwareEntryPoint
import com.konkuk.ma.support.security.WithAuthMember
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import

@Import(
    SecurityConfig::class,
    RoutingAwareEntryPoint::class,
    JwtManager::class,
    TestIdObfuscatorConfig::class,
    MemberQueryServiceTestConfig::class
)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@Target(AnnotationTarget.CLASS)
@Retention
@WithAuthMember
annotation class BaseApiTest
