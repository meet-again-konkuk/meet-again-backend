package com.konkuk.ma.auth

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.auth.exception.AuthTokenException
import com.konkuk.ma.domain.auth.exception.JwtExceptionType
import com.konkuk.ma.exception.BusinessException.LogLevel
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

    override fun generateAccessToken(memberId: Long): String {
        val generated = generateJwt(memberId, accessTokenExpiration)
        return generated.token
    }

    override fun generateRefreshToken(memberId: Long): RefreshToken {
        val generated = generateJwt(memberId, refreshTokenExpiration)
        return RefreshToken(
            memberId = memberId,
            expirationDate = generated.expiry,
            token = generated.token
        )
    }

    private data class GeneratedJwt(
        val token: String,
        val expiry: LocalDateTime
    )

    private fun generateJwt(memberId: Long, expirationMs: Long): GeneratedJwt {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val expiry = now.plus(Duration.ofMillis(expirationMs))

        val issuedDate = Date.from(now.atZone(zone).toInstant())
        val expiryDate = Date.from(expiry.atZone(zone).toInstant())

        val token = generateToken(memberId, issuedDate, expiryDate)
        return GeneratedJwt(token = token, expiry = expiry)
    }

    private fun generateToken(memberId: Long, issuedAt: Date, expiryDate: Date): String {
        return Jwts.builder()
            .setSubject(memberId.toString())
            .setIssuedAt(issuedAt)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    override fun validateToken(token: String): Boolean {
        getClaimsFromToken(token)
        return true
    }

    override fun getMemberIdFromToken(token: String): Long {
        val claims = getClaimsFromToken(token)
        return claims.subject.toLong()
    }

    private fun getClaimsFromToken(token: String): Claims {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: ExpiredJwtException) {
            throw AuthTokenException(token, JwtExceptionType.EXPIRED, e, LogLevel.INFO)
        } catch (e: MalformedJwtException) {
            throw AuthTokenException(token, JwtExceptionType.MALFORMED, e, LogLevel.ERROR)
        } catch (e: UnsupportedJwtException) {
            throw AuthTokenException(token, JwtExceptionType.UNSUPPORTED, e, LogLevel.ERROR)
        } catch (e: SignatureException) {
            throw AuthTokenException(token, JwtExceptionType.SIGNATURE, e, LogLevel.ERROR)
        } catch (e: IllegalArgumentException) {
            throw AuthTokenException(token, JwtExceptionType.ILLEGAL_ARGUMENT, e, LogLevel.ERROR)
        } catch (e: JwtException) {
            throw AuthTokenException(token, JwtExceptionType.ETC, e, LogLevel.ERROR)
        }
    }
} 
