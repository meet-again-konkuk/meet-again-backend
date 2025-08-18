package com.konkuk.ma.auth

import com.konkuk.ma.auth.domain.RefreshToken
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration
import java.time.LocalDateTime

class JwtGeneratorTest : FunSpec({

    val secret = "a".repeat(64) // 512-bit key for HS256

    test("액세스 토큰은 생성되고 유효하며, 이메일(subject)이 일치한다") {
        val generator = JwtGenerator(
            secretKey = secret,
            accessTokenExpiration = 5_000L,
            refreshTokenExpiration = 30_000L
        )

        val email = "user@example.com"
        val token = generator.generateAccessToken(email)

        generator.validateToken(token).shouldBeTrue()
        generator.getEmailFromToken(token) shouldBe email
    }

    test("리프레시 토큰은 값 객체로 반환되고, 만료 시각이 적절하다") {
        val generator = JwtGenerator(
            secretKey = secret,
            accessTokenExpiration = 5_000L,
            refreshTokenExpiration = 10_000L
        )

        val before = LocalDateTime.now()
        val refresh: RefreshToken = generator.generateRefreshToken("user@example.com")

        refresh.token shouldNotBe null
        generator.validateToken(refresh.token).shouldBeTrue()

        val elapsedMs = Duration.between(before, refresh.expirationDate).toMillis()
        elapsedMs.shouldBeBetween(9_000L, 11_000L)
    }

    test("형식이 잘못된 토큰은 검증에 실패한다") {
        val generator = JwtGenerator(
            secretKey = secret,
            accessTokenExpiration = 5_000L,
            refreshTokenExpiration = 10_000L
        )

        val invalidToken = "not-a-jwt"

        shouldThrow<MalformedJwtException> { generator.validateToken(invalidToken) }
        shouldThrow<MalformedJwtException> { generator.getEmailFromToken(invalidToken) }
    }

    test("만료된 토큰은 검증에 실패하고, 이메일 추출 시 예외가 발생한다") {
        val generator = JwtGenerator(
            secretKey = secret,
            accessTokenExpiration = 50L,
            refreshTokenExpiration = 10_000L
        )

        val email = "user@example.com"
        val token = generator.generateAccessToken(email)

        // 경계 시간 문제 방지를 위해 만료 시간을 충분히 초과하여 대기
        Thread.sleep(200)

        shouldThrow<ExpiredJwtException> { generator.validateToken(token) }
        shouldThrow<ExpiredJwtException> { generator.getEmailFromToken(token) }
    }

    test("만료된 리프레시 토큰은 이메일 추출 시 예외가 발생한다") {
        val generator = JwtGenerator(
            secretKey = secret,
            accessTokenExpiration = 5_000L,
            refreshTokenExpiration = 50L
        )

        val refresh = generator.generateRefreshToken("user@example.com")
        // 만료 시간을 충분히 초과하여 대기
        Thread.sleep(200)

        shouldThrow<ExpiredJwtException> { generator.validateToken(refresh.token) }
        shouldThrow<ExpiredJwtException> { generator.getEmailFromToken(refresh.token) }
    }
})
