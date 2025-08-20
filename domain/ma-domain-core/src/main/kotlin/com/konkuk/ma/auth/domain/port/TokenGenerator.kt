package com.konkuk.ma.auth.domain.port

import com.konkuk.ma.auth.domain.RefreshToken

interface TokenGenerator {
    fun generateAccessToken(email: String): String
    fun generateRefreshToken(email: String): RefreshToken
    fun validateToken(token: String): Boolean
    fun getEmailFromToken(token: String): String
}
