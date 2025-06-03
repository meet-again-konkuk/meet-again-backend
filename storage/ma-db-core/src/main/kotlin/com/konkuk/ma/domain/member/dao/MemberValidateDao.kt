package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.lowerCase
import org.springframework.stereotype.Component

@Component
class MemberValidateDao {
    fun existsByNickname(nickname: String): Boolean {
        return MemberTable.select(intLiteral(1))
            .where {
                (MemberTable.nickname.lowerCase() eq nickname.lowercase())
            }.limit(1)
            .firstOrNull() != null
    }

    fun existsByEmail(email: String): Boolean {
        return MemberTable.select(intLiteral(1))
            .where {
                (MemberTable.email.lowerCase() eq email.lowercase())
            }.limit(1)
            .firstOrNull() != null
    }
}
