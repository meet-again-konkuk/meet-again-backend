package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.intLiteral
import org.springframework.stereotype.Component

@Component
class MemberValidateDao {
    fun existsByNickname(nickname: String): Boolean {
        return MemberTable.select(intLiteral(1))
            .where {
                (MemberTable.nickname eq nickname)
            }.limit(1)
            .firstOrNull() != null
    }
}
