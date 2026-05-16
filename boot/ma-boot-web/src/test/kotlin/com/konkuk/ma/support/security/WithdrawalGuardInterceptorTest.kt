package com.konkuk.ma.support.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.konkuk.ma.domain.member.application.MemberQueryService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.method.HandlerMethod

class WithdrawalGuardInterceptorTest : FunSpec({

    val memberQueryService = mockk<MemberQueryService>()
    val mapper = ObjectMapper().registerModule(JavaTimeModule())
    val interceptor = WithdrawalGuardInterceptor(memberQueryService, mapper)

    beforeEach {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    afterEach { SecurityContextHolder.clearContext() }

    fun handlerMethodOf(method: String): HandlerMethod {
        val controller = TestController()
        return HandlerMethod(controller, TestController::class.java.getMethod(method))
    }

    fun authenticate(email: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(email, null, emptyList())
    }

    context("preHandle") {

        test("핸들러가 HandlerMethod가 아니면 통과시킨다") {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            interceptor.preHandle(request, response, Any()) shouldBe true
        }

        test("@SkipWithdrawalGuard 어노테이션이 붙은 메서드는 통과시킨다") {
            authenticate("user@example.com")
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            interceptor.preHandle(request, response, handlerMethodOf("skipped")) shouldBe true
        }

        test("인증된 사용자가 없으면 통과시킨다") {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            interceptor.preHandle(request, response, handlerMethodOf("guarded")) shouldBe true
        }

        test("활성 회원이면 통과시킨다") {
            authenticate("user@example.com")
            every { memberQueryService.isWithdrawalRequested("user@example.com") } returns false
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            interceptor.preHandle(request, response, handlerMethodOf("guarded")) shouldBe true
        }

        test("탈퇴 신청 회원이면 403을 반환하고 차단한다") {
            authenticate("user@example.com")
            every { memberQueryService.isWithdrawalRequested("user@example.com") } returns true
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            val result = interceptor.preHandle(request, response, handlerMethodOf("guarded"))

            result shouldBe false
            response.status shouldBe HttpServletResponse.SC_FORBIDDEN
        }
    }
})

private class TestController {
    @SkipWithdrawalGuard
    fun skipped() = Unit

    fun guarded() = Unit
}
