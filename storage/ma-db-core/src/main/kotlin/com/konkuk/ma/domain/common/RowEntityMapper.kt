package com.konkuk.ma.domain.common

import com.konkuk.ma.domain.auth.entity.RefreshTokenEntity
import com.konkuk.ma.domain.auth.entity.table.RefreshTokenTable
import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.ResultRow

object RowEntityMapper {
    fun toMemberEntity(row: ResultRow) = MemberEntity(
        id = row[MemberTable.id].value,
        email = row[MemberTable.email],
        password = row[MemberTable.password],
        nickname = row[MemberTable.nickname],
        phoneNumber = row[MemberTable.phoneNumber],
        name = row[MemberTable.name],
        region = enumValueOf(row[MemberTable.region]),
        birthDate = row[MemberTable.birthDate],
        highSchool = row[MemberTable.highSchool],
        university = row[MemberTable.university]
    )

    fun toRefreshTokenEntity(row: ResultRow) = RefreshTokenEntity(
        email = row[RefreshTokenTable.email],
        expirationDate = row[RefreshTokenTable.expirationDate],
        token = row[RefreshTokenTable.token],
    )
}
