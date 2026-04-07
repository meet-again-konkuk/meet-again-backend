package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.RefreshToken

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken)

    fun delete(email: String)

    fun findOne(email: String): RefreshToken
}
