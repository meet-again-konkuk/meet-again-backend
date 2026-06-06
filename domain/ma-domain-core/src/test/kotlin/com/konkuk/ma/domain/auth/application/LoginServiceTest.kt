package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.LoginCommand
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.auth.exception.PasswordMismatchException
import com.konkuk.ma.domain.auth.fixture.RefreshTokenFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.exception.WithdrawalPendingLoginException
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class LoginServiceTest : FunSpec({

    val memberQueryRepository = mockk<MemberQueryRepository>()
    val passwordVerifier = mockk<PasswordVerifier>()
    val tokenManager = mockk<TokenManager>()
    val refreshTokenGenerator = mockk<RefreshTokenGenerator>()
    val service = LoginService(
        memberQueryRepository,
        passwordVerifier,
        tokenManager,
        refreshTokenGenerator,
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

            every { memberQueryRepository.findOne(loginCommand.email) } returns member
            every { passwordVerifier.verify(loginCommand.password, member) } returns Unit
            every { tokenManager.generateAccessToken(member.email) } returns accessToken
            every { refreshTokenGenerator.generate(member.email) } returns refreshToken

            // When
            val result = service.login(loginCommand)

            // Then
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

        test("탈퇴 신청 상태의 회원이 로그인하면 WithdrawalPendingLoginException이 발생한다") {
            // Given
            val member = MemberFixture.create()
            member.requestWithdrawal(LocalDateTime.now())
            val loginCommand = LoginCommand(member.email.value, "input-password")

            every { memberQueryRepository.findOne(loginCommand.email) } returns member
            every { passwordVerifier.verify(loginCommand.password, member) } returns Unit

            // When & Then
            shouldThrow<WithdrawalPendingLoginException> {
                service.login(loginCommand)
            }
        }
    }
})
