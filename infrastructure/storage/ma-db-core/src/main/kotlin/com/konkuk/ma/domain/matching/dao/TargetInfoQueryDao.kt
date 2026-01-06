package com.konkuk.ma.domain.matching.dao

import com.konkuk.ma.domain.common.RowEntityMapper
import com.konkuk.ma.domain.matching.entity.TargetInfoEntity
import com.konkuk.ma.domain.matching.entity.table.TargetInfoTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class TargetInfoQueryDao {
    fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfoEntity> {
        return TargetInfoTable.selectAll()
            .where { cursorId?.let { TargetInfoTable.id.greater(it) } ?: Op.TRUE }
            .orderBy(TargetInfoTable.id to SortOrder.ASC)
            .limit(size)
            .map { it -> RowEntityMapper.toTargetInfoEntity(it) }
    }
}
