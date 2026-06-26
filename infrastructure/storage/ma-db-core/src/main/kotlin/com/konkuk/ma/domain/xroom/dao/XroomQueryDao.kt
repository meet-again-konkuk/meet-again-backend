package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.entity.XroomEntity
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import org.springframework.stereotype.Component

@Component
class XroomQueryDao {
    fun find(ownerId: Long): List<XroomEntity> {
        return XroomTable
            .activeRows { XroomTable.ownerId eq ownerId }
            .map { XroomEntity.from(it) }
    }

    fun findByTargetInfoIds(targetInfoIds: Set<Long>): List<XroomEntity> {
        if (targetInfoIds.isEmpty()) return emptyList()
        return XroomTable
            .activeRows { XroomTable.targetInfoId inList targetInfoIds }
            .map { XroomEntity.from(it) }
    }

    fun findOne(xroomId: Long): XroomEntity? {
        return XroomTable
            .activeRows { XroomTable.id eq xroomId }
            .limit(1)
            .firstOrNull()
            ?.let { XroomEntity.from(it) }
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
