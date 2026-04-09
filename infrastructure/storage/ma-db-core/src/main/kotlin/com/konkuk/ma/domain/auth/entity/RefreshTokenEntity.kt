package com.konkuk.ma.domain.auth.entity

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.common.domain.Email
import java.time.LocalDateTime

class RefreshTokenEntity(
    val email: String,

    val expirationDate: LocalDateTime,

    val token: String
) {
    fun toDomain(): RefreshToken {
        return RefreshToken(
            email = Email(email),
            expirationDate = expirationDate,
            token = token
        )
    }
}
