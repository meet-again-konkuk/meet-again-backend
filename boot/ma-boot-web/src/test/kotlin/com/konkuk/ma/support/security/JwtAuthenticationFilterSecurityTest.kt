package com.konkuk.ma.support.security

import com.konkuk.ma.config.SecurityConfig
import com.konkuk.ma.support.id.TestIdObfuscatorConfig
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.auth.exception.AuthTokenException
import com.konkuk.ma.exception.BusinessException
import com.konkuk.ma.domain.auth.exception.JwtExceptionType
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.application.MemberQueryService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(TestProtectedController::class)
@Import(SecurityConfig::class, JwtAuthenticationFilter::class, RoutingAwareEntryPoint::class, TestIdObfuscatorConfig::class)
class JwtAuthenticationFilterSecurityTest(
    private val mockMvc: MockMvc,

    @MockkBean private val tokenManager: TokenManager,

    @MockkBean private val memberQueryService: MemberQueryService

) : FunSpec({

    test("Authorization 헤더 없으면 401 또는 403") {
        mockMvc.get("/protected")
            .andExpect { status { isUnauthorized() } }
    }

    test("유효한 토큰이면 정상 인증되고 200 OK") {
        val token = "valid-jwt"
        val member = MemberFixture.create(id = 42L, email = "user@example.com")
        every { tokenManager.getMemberIdFromToken(token) } returns member.id
        every { memberQueryService.findOne(member.id) } returns member

        mockMvc.get("/protected") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    test("만료된 토큰이면 401 Unauthorized") {
        val bad = "bad-token"
        every { tokenManager.getMemberIdFromToken(bad) } throws AuthTokenException(
            token = bad,
            jwtExceptionType = JwtExceptionType.EXPIRED,
            throwable = null,
            logLevel = BusinessException.LogLevel.INFO
        )

        mockMvc.get("/protected") {
            header("Authorization", "Bearer $bad")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    test("형식이 잘못된 토큰이면 400 Bad Request") {
        val malformed = "not-a-jwt"
        every { tokenManager.getMemberIdFromToken(malformed) } throws AuthTokenException(
            token = malformed,
            jwtExceptionType = JwtExceptionType.MALFORMED,
            throwable = null,
            logLevel = BusinessException.LogLevel.INFO
        )

        mockMvc.get("/protected") {
            header("Authorization", "Bearer $malformed")
        }.andExpect {
            status { isBadRequest() }
        }
    }
})

@RestController
private class TestProtectedController {
    @GetMapping("/protected")
    fun protected(): ResponseEntity<Void> = ResponseEntity.ok().build()
}
