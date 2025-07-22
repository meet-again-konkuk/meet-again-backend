package com.konkuk.ma.member.domain.port

interface TokenGenerator {
    fun generateAccessToken(email: String): String
    fun generateRefreshToken(email: String): String
    fun validateToken(token: String): Boolean
    fun getEmailFromToken(token: String): String
    fun getTokenType(token: String): String
}
