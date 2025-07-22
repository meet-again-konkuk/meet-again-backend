package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.select
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

    fun existsByEmail(email: String): Boolean {
        return MemberTable.select(intLiteral(1))
            .where {
                (MemberTable.email eq email)
            }.limit(1)
            .firstOrNull() != null
    }

    fun findByEmail(email: String): MemberEntity? {
        return MemberTable.select(
            MemberTable.email,
            MemberTable.password,
            MemberTable.nickname,
            MemberTable.phoneNumber,
            MemberTable.name,
            MemberTable.birthDate,
            MemberTable.highSchool,
            MemberTable.university
        ).where {
            (MemberTable.email eq email)
        }.limit(1)
            .firstOrNull()
            ?.let { row ->
                MemberEntity(
                    email = row[MemberTable.email],
                    password = row[MemberTable.password],
                    nickname = row[MemberTable.nickname],
                    phoneNumber = row[MemberTable.phoneNumber],
                    name = row[MemberTable.name],
                    birthDate = row[MemberTable.birthDate],
                    highSchool = row[MemberTable.highSchool],
                    university = row[MemberTable.university]
                )
            }
    }
}
