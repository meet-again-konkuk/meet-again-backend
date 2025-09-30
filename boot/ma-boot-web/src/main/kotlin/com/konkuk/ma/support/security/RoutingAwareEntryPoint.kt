package com.konkuk.ma.support.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping

@Component
class RoutingAwareEntryPoint(
    private val handlerMappings: List<HandlerMapping>,
) : AuthenticationEntryPoint {

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        val hasHandler = handlerMappings.any { mapping ->
            try {
                mapping.getHandler(request) != null
            } catch (_: Exception) {
                false
            }
        }

        if (!hasHandler) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        }
    }
}
