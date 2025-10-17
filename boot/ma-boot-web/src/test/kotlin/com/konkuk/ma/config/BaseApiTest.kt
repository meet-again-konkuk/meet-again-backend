package com.konkuk.ma.config

import com.konkuk.ma.auth.JwtManager
import com.konkuk.ma.support.security.RoutingAwareEntryPoint
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser

@Import(SecurityConfig::class, RoutingAwareEntryPoint::class, JwtManager::class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
@Target(AnnotationTarget.CLASS)
@Retention
@WithMockUser
annotation class BaseApiTest
