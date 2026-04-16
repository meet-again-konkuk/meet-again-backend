package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.entity.table.XroomTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class XroomCommandDao {
    fun save(newXroom: NewXroom): Long {
        return XroomTable.insertAndGetId {
            it[ownerEmail] = newXroom.ownerEmail.value
            it[targetInfoId] = newXroom.targetInfoId
            it[theme] = newXroom.theme.name
            it[createdBy] = newXroom.ownerEmail.value
            it[lastModifiedBy] = newXroom.ownerEmail.value
        }.value
    }
}
