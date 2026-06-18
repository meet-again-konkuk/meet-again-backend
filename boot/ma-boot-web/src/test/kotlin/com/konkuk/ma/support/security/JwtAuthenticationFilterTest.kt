package com.konkuk.ma.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.application.MemberQueryService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest : FunSpec({

    val tokenManager = mockk<TokenManager>()
    val memberQueryService = mockk<MemberQueryService>()
    val mapper = mockk<ObjectMapper>()
    val filter = JwtAuthenticationFilter(tokenManager, memberQueryService, mapper)

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

        test("유효한 토큰이면 회원을 조회해 MemberInfo를 principal로 세팅하고 체인을 통과한다") {
            // Given
            val jwt = "valid-jwt-token"
            val member = MemberFixture.create(id = 42L, email = "user@example.com", nickname = "tester")
            every { tokenManager.getMemberIdFromToken(jwt) } returns member.id
            every { memberQueryService.findOne(member.id) } returns member

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
            val principal = SecurityContextHolder.getContext().authentication.principal
            principal.shouldBeInstanceOf<MemberInfo>()
            principal.id shouldBe member.id
            principal.email shouldBe member.email.value
            principal.nickname shouldBe member.nickname
        }
    }
})
