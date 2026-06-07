package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.common.RowEntityMapper
import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.domain.member.entity.table.MemberTable


import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.intLiteral
import org.springframework.stereotype.Component

@Component
class MemberQueryDao {
    fun existsByNickname(nickname: String): Boolean {
        return MemberTable.select(intLiteral(1))
            .where {
                (MemberTable.deleted eq false) and (MemberTable.nickname eq nickname)
            }.limit(1)
            .any()
    }

    fun existsByEmail(email: String): Boolean {
        return MemberTable.select(intLiteral(1))
            .where {
                (MemberTable.deleted eq false) and (MemberTable.email eq email)
            }.limit(1)
            .any()
    }

    fun existsByPhoneNumber(phoneNumber: String): Boolean {
        return MemberTable.select(intLiteral(1))
            .where {
                (MemberTable.deleted eq false) and (MemberTable.phoneNumber eq phoneNumber)
            }.limit(1)
            .any()
    }

    fun findOne(email: String): MemberEntity? {
        return MemberTable
            .activeRows { MemberTable.email eq email }
            .limit(1)
            .firstOrNull()
            ?.let { RowEntityMapper.toMemberEntity(it) }
    }

    fun findByEmails(emails: Set<String>): List<MemberEntity> {
        if (emails.isEmpty()) return emptyList()
        return MemberTable
            .activeRows { MemberTable.email inList emails }
            .map { RowEntityMapper.toMemberEntity(it) }
    }

    fun findByNames(names: Set<String>): List<MemberEntity> {
        if (names.isEmpty()) return emptyList()
        return MemberTable
            .activeRows { MemberTable.name inList names }
            .map { RowEntityMapper.toMemberEntity(it) }
    }
}
