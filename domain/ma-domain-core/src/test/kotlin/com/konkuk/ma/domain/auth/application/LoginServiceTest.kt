package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.LoginCommand
import com.konkuk.ma.domain.auth.domain.AuthTokenIssuer
import com.konkuk.ma.domain.auth.domain.AuthTokens
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.auth.fixture.RefreshTokenFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class LoginServiceTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val passwordVerifier = mockk<PasswordVerifier>()
    val authTokenIssuer = mockk<AuthTokenIssuer>()
    val service = LoginService(
        memberQueryRepository,
        passwordVerifier,
        authTokenIssuer,
    )

    beforeEach {
        clearAllMocks()
    }

    context("login") {

        test("로그인 성공 시 LoginInfo를 반환한다") {
            // Given
            val member = MemberFixture.create()
            val loginCommand = LoginCommand(member.email.value, "input-password")
            val accessToken = "test-access-token"
            val refreshToken = RefreshTokenFixture.create(email = member.email.value)
            val authTokens = AuthTokens(accessToken, refreshToken)

            every { memberQueryRepository.findOne(loginCommand.email) } returns member
            every { passwordVerifier.verify(loginCommand.password, member) } returns Unit
            every { authTokenIssuer.issueFor(member.email) } returns authTokens

            // When
            val result = service.login(loginCommand)

            // Then
            result.shouldBeInstanceOf<LoginInfo.Active>()
            result.email shouldBe member.email
            result.nickname shouldBe member.nickname
            result.accessToken shouldBe accessToken
            result.refreshToken shouldBe refreshToken
        }

        test("존재하지 않는 이메일로 로그인 시 예외가 발생한다") {
            // Given
            val loginCommand = LoginCommand("notfound@example.com", "any-password")

            every { memberQueryRepository.findOne(loginCommand.email) } throws
                EntityNotFoundException(com.konkuk.ma.exception.EntityType.MEMBER, loginCommand.email.value)

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.login(loginCommand)
            }
        }

        test("비밀번호가 일치하지 않으면 PasswordMismatchException이 발생한다") {
            // Given
            val member = MemberFixture.create()
            val loginCommand = LoginCommand(member.email.value, "wrong-password")

            every { memberQueryRepository.findOne(loginCommand.email) } returns member
            every { passwordVerifier.verify(loginCommand.password, member) } throws
                PasswordMismatchException(member.email)

            // When & Then
            shouldThrow<PasswordMismatchException> {
                service.login(loginCommand)
            }.message shouldBe "비밀번호가 올바르지 않습니다."
        }
    }
})
