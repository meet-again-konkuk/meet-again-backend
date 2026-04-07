package com.konkuk.ma.domain.auth.repository

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.dao.RefreshTokenDao
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenCoreRepository(
    private val refreshTokenDao: RefreshTokenDao
) : RefreshTokenRepository {
    override fun save(refreshToken: RefreshToken) {
        refreshTokenDao.save(refreshToken)
    }

    override fun delete(email: String) {
        refreshTokenDao.delete(email)
    }

    override fun findOne(email: String): RefreshToken {
        return refreshTokenDao.findOne(email)
            .toDomain()
    }
}
