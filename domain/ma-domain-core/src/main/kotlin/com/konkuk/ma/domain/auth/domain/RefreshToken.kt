package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.isExpired
import java.time.LocalDateTime

class RefreshToken(
    val email: Email,

    val expirationDate: LocalDateTime,

    val token: String,
) {
    fun isExpired(): Boolean {
        return expirationDate.isExpired()
    }
}
