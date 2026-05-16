package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.entity.MatchingResultEntity
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun find(targetInfoIds: List<Long>): List<MatchingResultEntity> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .join(MemberTable, JoinType.INNER, MatchingResultTable.targetEmail, MemberTable.email)
            .selectAll()
            .where {
                (MatchingResultTable.deleted eq false) and
                    (MatchingResultTable.targetInfoId inList targetInfoIds) and
                    MemberTable.withdrawalRequestedAt.isNull()
            }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun find(email: String, excluded: Boolean = false): List<MatchingResultEntity> {
        return MatchingResultTable
            .join(MemberTable, JoinType.INNER, MatchingResultTable.targetEmail, MemberTable.email)
            .selectAll()
            .where {
                (MatchingResultTable.deleted eq false) and
                    (MatchingResultTable.registerEmail eq email) and
                    (MatchingResultTable.excluded eq excluded) and
                    MemberTable.withdrawalRequestedAt.isNull()
            }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun exists(targetInfoId: Long): Boolean {
        return MatchingResultTable.select(intLiteral(1))
            .where { (MatchingResultTable.deleted eq false) and (MatchingResultTable.targetInfoId eq targetInfoId) }
            .limit(1)
            .any()
    }

    fun findClaimedByTarget(email: String): List<MatchingResultEntity> {
        return MatchingResultTable
            .join(MemberTable, JoinType.INNER, MatchingResultTable.registerEmail, MemberTable.email)
            .selectAll()
            .where {
                (MatchingResultTable.deleted eq false) and
                    (MatchingResultTable.targetEmail eq email) and
                    (MatchingResultTable.claimed eq true) and
                    MemberTable.withdrawalRequestedAt.isNull()
            }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun findOne(id: Long): MatchingResultEntity? {
        return MatchingResultTable
            .activeRows { MatchingResultTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let { MatchingResultEntity.from(it) }
    }
}
