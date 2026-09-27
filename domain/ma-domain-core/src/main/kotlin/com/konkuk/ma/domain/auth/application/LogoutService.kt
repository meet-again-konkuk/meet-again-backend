package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LogoutService(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun logout(memberId: Long) {
        refreshTokenRepository.delete(memberId)
    }
}
