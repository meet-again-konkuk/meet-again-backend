package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.RefreshToken

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken)

    fun delete(memberId: Long)

    fun findOne(memberId: Long): RefreshToken
}
