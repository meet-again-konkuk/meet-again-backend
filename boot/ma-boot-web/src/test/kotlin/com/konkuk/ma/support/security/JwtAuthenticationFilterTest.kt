package com.konkuk.ma.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest : FunSpec({

    lateinit var tokenManager: TokenManager
    lateinit var filter: JwtAuthenticationFilter
    lateinit var mapper: ObjectMapper

    beforeTest {
        tokenManager = mockk()
        mapper = mockk()
        filter = JwtAuthenticationFilter(tokenManager, mapper)
        SecurityContextHolder.clearContext()
    }

    test("Authorization 헤더가 없으면 체인을 그대로 통과한다") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { request, response -> chainCalled = true }

        filter.doFilter(request, response, chain)

        chainCalled.shouldBeTrue()
    }

    test("유효한 토큰이면 이메일을 추출하고 체인을 통과한다") {
        val jwt = "valid-jwt-token"
        val email = "user@example.com"
        every { tokenManager.getEmailFromToken(jwt) } returns email

        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $jwt")
        }
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { request, response -> chainCalled = true }

        filter.doFilter(request, response, chain)

        chainCalled.shouldBeTrue()
        // 구현에 따라 Authentication 설정 검증 (현재 구현은 이메일을 그대로 설정)
        SecurityContextHolder.getContext().authentication.principal shouldBe email
    }
})


