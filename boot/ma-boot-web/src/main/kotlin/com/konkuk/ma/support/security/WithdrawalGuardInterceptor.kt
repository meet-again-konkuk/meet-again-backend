package com.konkuk.ma.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.member.application.MemberQueryService
import com.konkuk.ma.support.payload.response.ApiError
import com.konkuk.ma.support.payload.response.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
@ConditionalOnBean(MemberQueryService::class)
class WithdrawalGuardInterceptor(
    private val memberQueryService: MemberQueryService,
    private val mapper: ObjectMapper
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) return true
        if (isSkipped(handler)) return true

        val email = currentEmail() ?: return true
        if (!memberQueryService.isWithdrawalRequested(email)) return true

        writeForbidden(response)
        return false
    }

    private fun isSkipped(handler: HandlerMethod): Boolean {
        return handler.hasMethodAnnotation(SkipWithdrawalGuard::class.java) ||
            handler.beanType.isAnnotationPresent(SkipWithdrawalGuard::class.java)
    }

    private fun currentEmail(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!authentication.isAuthenticated) return null
        return authentication.principal as? String
    }

    private fun writeForbidden(response: HttpServletResponse) {
        val apiError = ApiError(ErrorCode.WITHDRAWAL_PENDING)
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.use { out ->
            out.write(mapper.writeValueAsString(apiError))
            out.flush()
        }
    }
}
