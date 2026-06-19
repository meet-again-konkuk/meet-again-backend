package com.konkuk.ma.domain.auth.repository

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.dao.RefreshTokenDao
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

    override fun delete(memberId: Long) {
        refreshTokenDao.delete(memberId)
    }

    override fun findOne(memberId: Long): RefreshToken {
        return refreshTokenDao.findOne(memberId)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.REFRESH_TOKEN, memberId.toString())
    }
}
