package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.common.RowEntityMapper
import com.konkuk.ma.domain.point.entity.PointProductEntity
import com.konkuk.ma.domain.point.entity.table.PointProductTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.springframework.stereotype.Component

@Component
class PointProductQueryDao {
    fun find(): List<PointProductEntity> {
        return PointProductTable
            .activeRows { Op.TRUE }
            .orderBy(PointProductTable.displayOrder to SortOrder.ASC)
            .map { RowEntityMapper.toPointProductEntity(it) }
    }
}
