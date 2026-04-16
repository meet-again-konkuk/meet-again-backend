package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.exception.RefreshTokenExpiredException
import com.konkuk.ma.domain.auth.fixture.RefreshTokenFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class RefreshTokenValidatorTest : FunSpec({

    val refreshTokenRepository = mockk<RefreshTokenRepository>()
    val refreshTokenValidator = RefreshTokenValidator(refreshTokenRepository)

    context("validate") {

        test("만료되지 않은 토큰이면 예외 없이 통과한다") {
            // Given
            val refreshToken = RefreshTokenFixture.create(
                expirationDate = LocalDateTime.now().plusDays(7),
            )

            // When & Then
            refreshTokenValidator.validate(refreshToken)
        }

        test("만료된 토큰이면 delete 후 RefreshTokenExpiredException이 발생한다") {
            // Given
            val refreshToken = RefreshTokenFixture.create(
                expirationDate = LocalDateTime.now().minusDays(1),
            )

            every { refreshTokenRepository.delete(refreshToken.email) } returns Unit

            // When & Then
            shouldThrow<RefreshTokenExpiredException> {
                refreshTokenValidator.validate(refreshToken)
            }.message shouldBe "Refresh token이 만료되었습니다."

            verify { refreshTokenRepository.delete(refreshToken.email) }
        }
    }
})
