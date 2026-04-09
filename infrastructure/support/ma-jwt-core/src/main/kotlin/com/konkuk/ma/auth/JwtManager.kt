package com.konkuk.ma.auth

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.auth.exception.AuthTokenException
import com.konkuk.ma.domain.auth.exception.JwtExceptionType
import com.konkuk.ma.exception.BusinessException.LogLevel
import com.konkuk.ma.logger
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtManager(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) : TokenManager {

    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    override fun generateAccessToken(email: Email): String {
        logger.info { "Generating access token for email: ${email.value}" }
        val generated = generateJwt(email.value, accessTokenExpiration)
        return generated.token
    }

    override fun generateRefreshToken(email: Email): RefreshToken {
        logger.info { "Generating refresh token for email: ${email.value}" }
        val generated = generateJwt(email.value, refreshTokenExpiration)
        return RefreshToken(
            email = email,
            expirationDate = generated.expiry,
            token = generated.token
        )
    }

    private data class GeneratedJwt(
        val token: String,
        val expiry: LocalDateTime
    )

    private fun generateJwt(email: String, expirationMs: Long): GeneratedJwt {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val expiry = now.plus(Duration.ofMillis(expirationMs))

        val issuedDate = Date.from(now.atZone(zone).toInstant())
        val expiryDate = Date.from(expiry.atZone(zone).toInstant())

        val token = generateToken(email, issuedDate, expiryDate)
        logger.info { "generated token: $token, email: $email" }
        return GeneratedJwt(token = token, expiry = expiry)
    }

    private fun generateToken(email: String, issuedAt: Date, expiryDate: Date): String {
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(issuedAt)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun validateToken(token: String): Boolean {
        getClaimsFromToken(token)
        return true
    }

    override fun getEmailFromToken(token: String): String {
        val claims = getClaimsFromToken(token)
        return claims.subject
    }

    private fun getClaimsFromToken(token: String): Claims {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: ExpiredJwtException) {
            throw AuthTokenException(token, JwtExceptionType.EXPIRED, e, LogLevel.INFO, this::getClaimsFromToken)
        } catch (e: MalformedJwtException) {
            throw AuthTokenException(token, JwtExceptionType.MALFORMED, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: UnsupportedJwtException) {
            throw AuthTokenException(token, JwtExceptionType.UNSUPPORTED, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: SignatureException) {
            throw AuthTokenException(token, JwtExceptionType.SIGNATURE, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: IllegalArgumentException) {
            throw AuthTokenException(token, JwtExceptionType.ILLEGAL_ARGUMENT, e, LogLevel.ERROR, this::getClaimsFromToken)
        } catch (e: JwtException) {
            throw AuthTokenException(token, JwtExceptionType.ETC, e, LogLevel.ERROR, this::getClaimsFromToken)
        }
    }
} 
