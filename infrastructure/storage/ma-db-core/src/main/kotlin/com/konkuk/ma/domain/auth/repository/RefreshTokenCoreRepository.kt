package com.konkuk.ma.domain.auth.repository

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.dao.RefreshTokenDao
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenCoreRepository(
    private val refreshTokenDao: RefreshTokenDao
) : RefreshTokenRepository {
    override fun save(refreshToken: RefreshToken) {
        refreshTokenDao.save(refreshToken)
    }

    override fun delete(email: Email) {
        refreshTokenDao.delete(email.value)
    }

    override fun findOne(email: Email): RefreshToken {
        return refreshTokenDao.findOne(email.value)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.REFRESH_TOKEN, email.value)
    }
}
