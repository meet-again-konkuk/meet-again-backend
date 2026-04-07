package com.konkuk.ma.domain.member.dao

import com.konkuk.ma.domain.common.RowEntityMapper
import com.konkuk.ma.domain.member.entity.MemberEntity
import com.konkuk.ma.domain.member.entity.table.MemberTable
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MemberQueryDao {
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

    fun findOne(email: String): MemberEntity {
        return MemberTable.selectAll()
            .where { (MemberTable.email eq email) }
            .limit(1)
            .firstOrNull()
            ?.let { RowEntityMapper.toMemberEntity(it) }
            ?: throw EntityNotFoundException(EntityType.MEMBER, email)
    }

    fun findByEmails(emails: Set<String>): List<MemberEntity> {
        if (emails.isEmpty()) return emptyList()
        return MemberTable.selectAll()
            .where { (MemberTable.email inList emails) and (MemberTable.deleted eq false) }
            .map { RowEntityMapper.toMemberEntity(it) }
    }

    fun findByNames(names: Set<String>): List<MemberEntity> {
        if (names.isEmpty()) return emptyList()
        return MemberTable.selectAll()
            .where { MemberTable.name inList names }
            .map { RowEntityMapper.toMemberEntity(it) }
    }
}
