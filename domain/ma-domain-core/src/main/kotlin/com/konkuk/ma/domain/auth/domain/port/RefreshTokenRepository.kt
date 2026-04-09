package com.konkuk.ma.domain.auth.domain.port

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.common.domain.Email

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken)

    fun delete(email: Email)

    fun findOne(email: Email): RefreshToken
}
