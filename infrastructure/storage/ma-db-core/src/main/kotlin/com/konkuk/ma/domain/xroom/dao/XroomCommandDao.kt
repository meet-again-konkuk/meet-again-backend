package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class XroomCommandDao {
    fun save(newXroom: NewXroom): Long {
        return XroomTable.insertAndGetId {
            it[ownerId] = newXroom.ownerId
            it[targetInfoId] = newXroom.targetInfoId
            it[theme] = newXroom.theme.name
            it[createdBy] = newXroom.ownerId.toString()
            it[lastModifiedBy] = newXroom.ownerId.toString()
        }.value
    }

    fun delete(ownerId: Long) {
        XroomTable.softDelete({ XroomTable.ownerId eq ownerId }, ownerId.toString())
    }
}
