package com.konkuk.ma.support.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithAuthMemberSecurityContextFactory : WithSecurityContextFactory<WithAuthMember> {
    override fun createSecurityContext(annotation: WithAuthMember): SecurityContext {
        val memberInfo = MemberInfo(
            id = annotation.id,
            email = annotation.email,
            nickname = annotation.nickname,
        )
        val context = SecurityContextHolder.getContext()
        context.authentication = UsernamePasswordAuthenticationToken(memberInfo, null, emptyList<SimpleGrantedAuthority>())
        return context
    }
}
