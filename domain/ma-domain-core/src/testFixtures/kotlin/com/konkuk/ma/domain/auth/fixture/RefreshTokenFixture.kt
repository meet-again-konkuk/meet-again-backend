package com.konkuk.ma.domain.auth.fixture

import com.konkuk.ma.domain.auth.domain.RefreshToken
import java.time.LocalDateTime

object RefreshTokenFixture {
    fun create(
        memberId: Long = 1L,
        expirationDate: LocalDateTime = LocalDateTime.now().plusDays(7),
        token: String = "test-refresh-token",
    ): RefreshToken {
        return RefreshToken(
            memberId = memberId,
            expirationDate = expirationDate,
            token = token,
        )
    }
}
