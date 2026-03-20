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

    val tokenManager = mockk<TokenManager>()
    val mapper = mockk<ObjectMapper>()
    val filter = JwtAuthenticationFilter(tokenManager, mapper)

    beforeTest {
        SecurityContextHolder.clearContext()
    }

    context("doFilter") {

        test("Authorization 헤더가 없으면 체인을 그대로 통과한다") {
            // Given
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            var chainCalled = false
            val chain = FilterChain { _, _ -> chainCalled = true }

            // When
            filter.doFilter(request, response, chain)

            // Then
            chainCalled.shouldBeTrue()
        }

        test("유효한 토큰이면 이메일을 추출하고 체인을 통과한다") {
            // Given
            val jwt = "valid-jwt-token"
            val email = "user@example.com"
            every { tokenManager.getEmailFromToken(jwt) } returns email

            val request = MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer $jwt")
            }
            val response = MockHttpServletResponse()
            var chainCalled = false
            val chain = FilterChain { _, _ -> chainCalled = true }

            // When
            filter.doFilter(request, response, chain)

            // Then
            chainCalled.shouldBeTrue()
            SecurityContextHolder.getContext().authentication.principal shouldBe email
        }
    }
})
