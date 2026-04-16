package com.konkuk.ma.domain.auth.fixture

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.common.domain.Email
import java.time.LocalDateTime

object RefreshTokenFixture {
    fun create(
        email: String = "test@example.com",
        expirationDate: LocalDateTime = LocalDateTime.now().plusDays(7),
        token: String = "test-refresh-token",
    ): RefreshToken {
        return RefreshToken(
            email = Email(email),
            expirationDate = expirationDate,
            token = token,
        )
    }
}
