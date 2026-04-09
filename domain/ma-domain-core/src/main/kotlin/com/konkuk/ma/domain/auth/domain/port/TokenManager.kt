package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.common.domain.Email

interface TokenManager {
    fun generateAccessToken(email: Email): String
    fun generateRefreshToken(email: Email): RefreshToken
    fun validateToken(token: String): Boolean
    fun getEmailFromToken(token: String): String
}
