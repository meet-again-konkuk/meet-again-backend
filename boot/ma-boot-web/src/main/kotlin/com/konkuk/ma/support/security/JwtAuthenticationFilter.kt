package com.konkuk.ma.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.auth.exception.AuthTokenException
import com.konkuk.ma.support.payload.response.ApiError
import com.konkuk.ma.support.payload.response.ErrorCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenManager: TokenManager,

    private val mapper: ObjectMapper
) : OncePerRequestFilter() {
    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }
        val jwt = authHeader.substring(BEARER_PREFIX.length)
        try {
            val memberId = tokenManager.getMemberIdFromToken(jwt)
            val authentication = UsernamePasswordAuthenticationToken(memberId, null, emptyList<SimpleGrantedAuthority>())
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authentication
        } catch (e: AuthTokenException) {
            handleAuthTokenException(e, response)
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun handleAuthTokenException(e: AuthTokenException, response: HttpServletResponse) {
        when {
            e.isExpired() -> writeApiError(response, ErrorCode.EXPIRED_TOKEN, HttpServletResponse.SC_UNAUTHORIZED)
            e.isMalformed() -> writeApiError(response, ErrorCode.MALFORMED_TOKEN, HttpServletResponse.SC_BAD_REQUEST)
            e.isInvalid() -> writeApiError(response, ErrorCode.INVALID_TOKEN, HttpServletResponse.SC_BAD_REQUEST)
            e.isOtherError() -> writeApiError(response, ErrorCode.OTHER_TOKEN_ERROR, HttpServletResponse.SC_BAD_REQUEST)
        }
    }

    private fun writeApiError(
        response: HttpServletResponse,
        errorCode: ErrorCode,
        httpStatus: Int,
    ) {
        val apiError = ApiError(errorCode)
        response.status = httpStatus
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.use { out ->
            out.write(mapper.writeValueAsString(apiError))
            out.flush()
        }
    }
}
