package com.konkuk.ma.auth

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.exception.AuthTokenException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

class JwtGeneratorTest : FunSpec({

    val secret = "a".repeat(64) // 512-bit key for HS256

    context("generateAccessToken") {

        test("액세스 토큰은 생성되고 유효하며, 이메일(subject)이 일치한다") {
            // Given
            val generator = JwtManager(
                secretKey = secret,
                accessTokenExpiration = 5_000L,
                refreshTokenExpiration = 30_000L
            )
            val email = "user@example.com"

            // When
            val token = generator.generateAccessToken(email)

            // Then
            generator.validateToken(token).shouldBeTrue()
            generator.getEmailFromToken(token) shouldBe email
        }
    }

    context("generateRefreshToken") {

        test("리프레시 토큰은 값 객체로 반환되고, 만료 시각이 적절하다") {
            // Given
            val generator = JwtManager(
                secretKey = secret,
                accessTokenExpiration = 5_000L,
                refreshTokenExpiration = 10_000L
            )

            // When
            val before = LocalDateTime.now()
            val refresh: RefreshToken = generator.generateRefreshToken("user@example.com")

            // Then
            refresh.token shouldNotBe null
            generator.validateToken(refresh.token).shouldBeTrue()

            val elapsedMs = Duration.between(before, refresh.expirationDate).toMillis()
            elapsedMs.shouldBeBetween(9_000L, 11_000L)
        }

        test("리프레시 토큰의 만료 시각과 JWT exp 클레임이 동일하다") {
            // Given
            val generator = JwtManager(
                secretKey = secret,
                accessTokenExpiration = 5_000L,
                refreshTokenExpiration = 10_000L
            )

            // When
            val refresh = generator.generateRefreshToken("user@example.com")

            // Then
            val parser = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.toByteArray()))
                .build()
            val jwtExpInstant = parser.parseClaimsJws(refresh.token).body.expiration.toInstant()
            val refreshExpInstant = refresh.expirationDate.atZone(ZoneId.systemDefault()).toInstant()

            jwtExpInstant.epochSecond shouldBe refreshExpInstant.epochSecond
        }

        test("만료된 리프레시 토큰은 이메일 추출 시 예외가 발생하고 isExpired() 메소드는 true를 리턴한다.") {
            // Given
            val generator = JwtManager(
                secretKey = secret,
                accessTokenExpiration = 5_000L,
                refreshTokenExpiration = 500L
            )
            val refresh = generator.generateRefreshToken("user@example.com")

            // When
            // 만료 시간을 충분히 초과하여 대기
            Thread.sleep(500)

            // Then
            shouldThrow<AuthTokenException> { generator.validateToken(refresh.token) }
            refresh.isExpired() shouldBe true
        }
    }

    context("validateToken") {

        test("형식이 잘못된 토큰은 검증에 실패한다") {
            // Given
            val generator = JwtManager(
                secretKey = secret,
                accessTokenExpiration = 5_000L,
                refreshTokenExpiration = 10_000L
            )
            val invalidToken = "not-a-jwt"

            // When & Then
            shouldThrow<AuthTokenException> { generator.validateToken(invalidToken) }
            shouldThrow<AuthTokenException> { generator.getEmailFromToken(invalidToken) }
        }

        test("만료된 토큰은 검증에 실패하고, 이메일 추출 시 예외가 발생한다") {
            // Given
            val generator = JwtManager(
                secretKey = secret,
                accessTokenExpiration = 50L,
                refreshTokenExpiration = 10_000L
            )
            val email = "user@example.com"
            val token = generator.generateAccessToken(email)

            // When
            // 경계 시간 문제 방지를 위해 만료 시간을 충분히 초과하여 대기
            Thread.sleep(200)

            // Then
            shouldThrow<AuthTokenException> { generator.validateToken(token) }
            shouldThrow<AuthTokenException> { generator.getEmailFromToken(token) }
        }
    }
})
