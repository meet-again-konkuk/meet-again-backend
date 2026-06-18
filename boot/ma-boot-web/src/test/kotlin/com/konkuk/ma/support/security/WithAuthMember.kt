package com.konkuk.ma.support.security

import org.springframework.security.test.context.support.WithSecurityContext

@Target(AnnotationTarget.CLASS)
@Retention
@WithSecurityContext(factory = WithAuthMemberSecurityContextFactory::class)
annotation class WithAuthMember(
    val id: Long = 1L
)
