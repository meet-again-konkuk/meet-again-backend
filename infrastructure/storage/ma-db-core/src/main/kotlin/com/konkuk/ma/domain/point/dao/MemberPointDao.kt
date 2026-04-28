package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.entity.MemberPointEntity
import com.konkuk.ma.domain.point.entity.table.MemberPointTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class MemberPointDao {
    fun findOneForUpdateOrNull(ownerEmail: String): MemberPointEntity? {
        return MemberPointTable
            .selectAll()
            .where { (MemberPointTable.ownerEmail eq ownerEmail) and (MemberPointTable.deleted eq false) }
            .limit(1)
            .forUpdate()
            .firstOrNull()
            ?.let { MemberPointEntity.from(it) }
    }

    fun insert(entity: MemberPointEntity): Long {
        return MemberPointTable.insertAndGetId {
            it[ownerEmail] = entity.ownerEmail
            it[balance] = entity.balance
            it[createdBy] = entity.ownerEmail
            it[lastModifiedBy] = entity.ownerEmail
        }.value
    }

    fun updateBalance(id: Long, newBalance: Int) {
        MemberPointTable.update({ MemberPointTable.id eq id }) {
            it[balance] = newBalance
        }
    }
}
