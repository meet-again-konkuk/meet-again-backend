package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockType
import com.konkuk.ma.domain.xroom.entity.DdayBlockEntity
import com.konkuk.ma.domain.xroom.entity.XroomBlockEntity
import com.konkuk.ma.domain.xroom.entity.table.DdayBlockTable
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.springframework.stereotype.Component

@Component
class DdayBlockEntityFactory : XroomBlockEntityFactory {
    override val type: XroomBlockType = XroomBlockType.DDAY
    override val childTable: LongIdTable = DdayBlockTable

    override fun createFrom(row: ResultRow): XroomBlockEntity {
        return DdayBlockEntity(
            id = row[XroomBlockTable.id].value,
            xroomId = row[XroomBlockTable.xroomId],
            item = XroomBlockItem.valueOf(row[XroomBlockTable.item]),
            positionX = row[XroomBlockTable.positionX].toInt(),
            positionY = row[XroomBlockTable.positionY].toInt(),
            rotation = row[XroomBlockTable.rotation],
            anniversaryDate = row[DdayBlockTable.anniversaryDate],
            label = row[DdayBlockTable.label],
        )
    }
}
