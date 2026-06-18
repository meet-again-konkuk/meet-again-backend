package com.konkuk.ma.domain.auth.dao

import com.konkuk.ma.domain.auth.domain.RefreshToken
import com.konkuk.ma.domain.auth.entity.RefreshTokenEntity
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.common.RowEntityMapper


import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class RefreshTokenDao {
    fun save(refreshToken: RefreshToken) {
        RefreshTokenTable.insert {
            it[memberId] = refreshToken.memberId
            it[token] = refreshToken.token
            it[expirationDate] = refreshToken.expirationDate
            it[createdBy] = refreshToken.memberId.toString()
            it[lastModifiedBy] = refreshToken.memberId.toString()
        }
    }

    fun delete(memberId: Long) {
        RefreshTokenTable.deleteWhere {
            RefreshTokenTable.memberId eq memberId
        }
    }

    fun findOne(memberId: Long): RefreshTokenEntity? {
        return RefreshTokenTable.selectAll()
            .where { RefreshTokenTable.memberId eq memberId }
            .limit(1)
            .firstOrNull()
            ?.let { RowEntityMapper.toRefreshTokenEntity(it) }
    }
}
