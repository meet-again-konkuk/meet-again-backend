package com.konkuk.ma.domain.auth.entity

import com.konkuk.ma.domain.auth.domain.RefreshToken
import java.time.LocalDateTime

class RefreshTokenEntity(
    val memberId: Long,

    val expirationDate: LocalDateTime,

    val token: String
) {
    fun toDomain(): RefreshToken {
        return RefreshToken(
            memberId = memberId,
            expirationDate = expirationDate,
            token = token
        )
    }
}
