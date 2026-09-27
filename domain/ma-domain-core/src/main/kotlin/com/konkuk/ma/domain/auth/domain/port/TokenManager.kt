package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.RefreshToken

interface TokenManager {
    fun generateAccessToken(memberId: Long): String
    fun generateRefreshToken(memberId: Long): RefreshToken
    fun validateToken(token: String): Boolean
    fun getMemberIdFromToken(token: String): Long
}
