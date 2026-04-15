package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.entity.MatchingResultEntity
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun find(targetInfoIds: List<Long>): List<MatchingResultEntity> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .selectAll()
            .where { MatchingResultTable.targetInfoId inList targetInfoIds }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun find(email: String, excluded: Boolean = false): List<MatchingResultEntity> {
        return MatchingResultTable
            .activeRows { (MatchingResultTable.registerEmail eq email) and (MatchingResultTable.excluded eq excluded) }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun exists(targetInfoId: Long): Boolean {
        return MatchingResultTable
            .activeRows { MatchingResultTable.targetInfoId eq targetInfoId }
            .limit(1)
            .any()
    }

    fun findOne(id: Long): MatchingResultEntity? {
        return MatchingResultTable
            .activeRows { MatchingResultTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let { MatchingResultEntity.from(it) }
    }
}
