package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.domain.auth.domain.RefreshTokenValidator
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.auth.exception.RefreshTokenExpiredException
import com.konkuk.ma.domain.auth.fixture.RefreshTokenFixture
import com.konkuk.ma.domain.matching.fixture.MemberFixture
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.member.exception.WithdrawalPendingLoginException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class RefreshTokenServiceTest : FunSpec({

    val tokenManager = mockk<TokenManager>()
    val refreshTokenRepository = mockk<RefreshTokenRepository>()
    val refreshTokenValidator = mockk<RefreshTokenValidator>()
    val refreshTokenGenerator = mockk<RefreshTokenGenerator>()
    val memberQueryRepository = mockk<MemberQueryRepository>()
    val service = RefreshTokenService(
        tokenManager,
        refreshTokenRepository,
        refreshTokenValidator,
        refreshTokenGenerator,
        memberQueryRepository,
    )

    beforeEach {
        clearAllMocks()
    }

    context("refreshToken") {

        test("유효한 리프레시 토큰으로 새 토큰을 발급한다") {
            // Given
            val refreshToken = RefreshTokenFixture.create()
            val member = MemberFixture.create(email = refreshToken.email.value)
            val inputToken = "input-refresh-token"
            val newAccessToken = "new-access-token"
            val newRefreshToken = RefreshTokenFixture.create(
                email = refreshToken.email.value,
                token = "new-refresh-token",
            )

            every { tokenManager.getEmailFromToken(inputToken) } returns refreshToken.email.value
            every { refreshTokenRepository.findOne(refreshToken.email) } returns refreshToken
            every { refreshTokenValidator.validate(refreshToken) } returns Unit
            every { memberQueryRepository.findOne(refreshToken.email) } returns member
            every { tokenManager.generateAccessToken(refreshToken.email) } returns newAccessToken
            every { refreshTokenGenerator.generate(refreshToken.email) } returns newRefreshToken

            // When
            val result = service.refreshToken(inputToken)

            // Then
            result.email shouldBe refreshToken.email
            result.nickname shouldBe member.nickname
            result.accessToken shouldBe newAccessToken
            result.refreshToken shouldBe newRefreshToken
        }

        test("만료된 리프레시 토큰이면 RefreshTokenExpiredException이 발생한다") {
            // Given
            val expiredRefreshToken = RefreshTokenFixture.create(
                expirationDate = LocalDateTime.now().minusDays(1),
            )
            val inputToken = "expired-refresh-token"

            every { tokenManager.getEmailFromToken(inputToken) } returns expiredRefreshToken.email.value
            every { refreshTokenRepository.findOne(expiredRefreshToken.email) } returns expiredRefreshToken
            every { refreshTokenValidator.validate(expiredRefreshToken) } throws
                RefreshTokenExpiredException(expiredRefreshToken.email, expiredRefreshToken.expirationDate.toString())

            // When & Then
            shouldThrow<RefreshTokenExpiredException> {
                service.refreshToken(inputToken)
            }.message shouldBe "Refresh token이 만료되었습니다."
        }

        test("탈퇴 신청 상태의 회원이 토큰을 갱신하면 WithdrawalPendingLoginException이 발생한다") {
            // Given
            val refreshToken = RefreshTokenFixture.create()
            val member = MemberFixture.create(email = refreshToken.email.value)
            member.requestWithdrawal(LocalDateTime.now())
            val inputToken = "input-refresh-token"

            every { tokenManager.getEmailFromToken(inputToken) } returns refreshToken.email.value
            every { refreshTokenRepository.findOne(refreshToken.email) } returns refreshToken
            every { refreshTokenValidator.validate(refreshToken) } returns Unit
            every { memberQueryRepository.findOne(refreshToken.email) } returns member

            // When & Then
            shouldThrow<WithdrawalPendingLoginException> {
                service.refreshToken(inputToken)
            }
        }
    }
})
