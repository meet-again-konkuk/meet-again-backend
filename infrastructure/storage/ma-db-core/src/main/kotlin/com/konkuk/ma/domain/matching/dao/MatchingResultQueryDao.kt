package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.matching.entity.MatchingResultEntity
import com.konkuk.ma.domain.matching.entity.table.MatchingResultTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MatchingResultQueryDao {
    fun findByTargetInfoIds(targetInfoIds: List<Long>): List<MatchingResultEntity> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return MatchingResultTable
            .selectAll()
            .where { MatchingResultTable.targetInfoId inList targetInfoIds }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun findByRegisterEmail(email: String): List<MatchingResultEntity> {
        return MatchingResultTable
            .selectAll()
            .where {
                (MatchingResultTable.registerEmail eq email) and
                    (MatchingResultTable.deleted eq false) and
                    (MatchingResultTable.excluded eq false)
            }
            .map { row -> MatchingResultEntity.from(row) }
    }

    fun findById(id: Long): MatchingResultEntity? {
        return MatchingResultTable
            .selectAll()
            .where {
                (MatchingResultTable.id eq id) and
                    (MatchingResultTable.deleted eq false)
            }
            .map { row -> MatchingResultEntity.from(row) }
            .singleOrNull()
    }
}
