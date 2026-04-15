package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.common.RowEntityMapper
import com.konkuk.ma.domain.matching.entity.TargetInfoEntity
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Component

@Component
class TargetInfoQueryDao {
    fun findOne(id: Long): TargetInfoEntity? {
        return TargetInfoTable
            .activeRows { TargetInfoTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let { RowEntityMapper.toTargetInfoEntity(it) }
    }

    fun find(email: String): List<TargetInfoEntity> {
        return TargetInfoTable
            .activeRows { TargetInfoTable.registerEmail eq email }
            .orderBy(TargetInfoTable.id to SortOrder.DESC)
            .map { RowEntityMapper.toTargetInfoEntity(it) }
    }

    fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfoEntity> {
        val query = TargetInfoTable.activeRows()
        if (cursorId != null) {
            query.adjustWhere { this!! and (TargetInfoTable.id greater cursorId) }
        }
        return query
            .orderBy(TargetInfoTable.id to SortOrder.ASC)
            .limit(size)
            .map { RowEntityMapper.toTargetInfoEntity(it) }
    }
}
