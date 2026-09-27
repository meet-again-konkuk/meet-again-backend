package com.konkuk.ma.support.security

import com.konkuk.ma.domain.member.application.MemberQueryService
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class LoginMemberArgumentResolver(
    private val memberQueryService: MemberQueryService,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(LoginMember::class.java) &&
            parameter.parameterType == MemberInfo::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): MemberInfo {
        val memberId = currentMemberId()
        val member = memberQueryService.findOne(memberId)
        return MemberInfo.from(member)
    }

    private fun currentMemberId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return principal as? Long
            ?: throw AuthenticationCredentialsNotFoundException("인증 정보가 존재하지 않습니다.")
    }
}
