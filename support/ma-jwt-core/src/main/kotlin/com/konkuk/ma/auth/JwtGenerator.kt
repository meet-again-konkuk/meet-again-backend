package com.konkuk.ma.auth

import com.konkuk.ma.auth.domain.RefreshToken
import com.konkuk.ma.auth.domain.port.TokenGenerator
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtGenerator(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) : TokenGenerator {

    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    override fun generateAccessToken(email: String): String {
        val generated = generateJwt(email, accessTokenExpiration)
        return generated.token
    }

    override fun generateRefreshToken(email: String): RefreshToken {
        val generated = generateJwt(email, refreshTokenExpiration)
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
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
        return true
    }

    override fun getEmailFromToken(token: String): String {
        val claims = getClaimsFromToken(token)
        return claims.subject
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
} 
