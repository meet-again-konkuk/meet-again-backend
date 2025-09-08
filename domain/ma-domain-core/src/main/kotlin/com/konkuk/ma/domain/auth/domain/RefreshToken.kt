package com.konkuk.ma.domain.auth.domain

import java.time.LocalDateTime

class RefreshToken(
    val email: String,

    val expirationDate: LocalDateTime,

    val token: String,
) {
    fun isExpired(): Boolean {
        return !LocalDateTime.now().isBefore(expirationDate)
    }
}
