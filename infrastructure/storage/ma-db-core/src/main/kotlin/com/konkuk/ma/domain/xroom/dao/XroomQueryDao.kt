package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.entity.XroomEntity
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import org.springframework.stereotype.Component

@Component
class XroomQueryDao {
    fun find(ownerEmail: String): List<XroomEntity> {
        return XroomTable
            .activeRows { XroomTable.ownerEmail eq ownerEmail }
            .map { XroomEntity.from(it) }
    }

    fun exists(targetInfoId: Long): Boolean {
        return XroomTable
            .activeRows { XroomTable.targetInfoId eq targetInfoId }
            .limit(1)
            .any()
    }

    fun exists(targetInfoIds: Set<Long>): Set<Long> {
        if (targetInfoIds.isEmpty()) return emptySet()
        return XroomTable
            .activeRows { XroomTable.targetInfoId inList targetInfoIds }
            .map { it[XroomTable.targetInfoId] }
            .toSet()
    }
}
