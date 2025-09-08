package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.RefreshToken

interface TokenManager {
    fun generateAccessToken(email: String): String
    fun generateRefreshToken(email: String): RefreshToken
    fun validateToken(token: String): Boolean
    fun getEmailFromToken(token: String): String
}
