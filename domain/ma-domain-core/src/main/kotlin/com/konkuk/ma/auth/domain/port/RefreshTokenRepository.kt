package com.konkuk.ma.auth.domain.port

import com.konkuk.ma.auth.domain.RefreshToken

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken)

    fun delete(email: String)

    fun findByEmail(email: String): RefreshToken
}
