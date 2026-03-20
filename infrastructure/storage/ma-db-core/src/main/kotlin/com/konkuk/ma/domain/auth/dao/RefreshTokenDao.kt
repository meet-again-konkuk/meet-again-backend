package com.konkuk.ma.domain.auth.dao

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.entity.RefreshTokenEntity
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.RowEntityMapper
import com.konkuk.ma.exception.EntityNotFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class RefreshTokenDao {
    fun save(refreshToken: RefreshToken) {
        RefreshTokenTable.insert {
            it[email] = refreshToken.email
            it[token] = refreshToken.token
            it[expirationDate] = refreshToken.expirationDate
            it[createdBy] = refreshToken.email
            it[lastModifiedBy] = refreshToken.email
        }
    }

    fun delete(email: String) {
        RefreshTokenTable.deleteWhere {
            RefreshTokenTable.email eq email
        }
    }

    fun findByEmail(email: String): RefreshTokenEntity {
        return RefreshTokenTable.selectAll()
            .where { RefreshTokenTable.email eq email }
            .limit(1)
            .firstOrNull()
            ?.let { RowEntityMapper.toRefreshTokenEntity(it) }
            ?: throw EntityNotFoundException("RefreshToken", "email", email)
    }
}
