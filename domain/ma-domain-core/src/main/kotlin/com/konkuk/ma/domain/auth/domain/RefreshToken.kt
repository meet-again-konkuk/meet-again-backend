package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.date.isExpired
import java.time.LocalDateTime

class RefreshToken(
    val memberId: Long,

    val expirationDate: LocalDateTime,

    val token: String,
) {
    fun isExpired(): Boolean {
        return expirationDate.isExpired()
    }
}
