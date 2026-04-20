package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockType
import com.konkuk.ma.domain.xroom.entity.LongTextBlockEntity
import com.konkuk.ma.domain.xroom.entity.XroomBlockEntity
import com.konkuk.ma.domain.xroom.entity.table.LongTextBlockTable
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.springframework.stereotype.Component

@Component
class LongTextBlockEntityFactory : XroomBlockEntityFactory {
    override val type: XroomBlockType = XroomBlockType.LONG_TEXT
    override val childTable: LongIdTable = LongTextBlockTable

    override fun createFrom(row: ResultRow): XroomBlockEntity {
        return LongTextBlockEntity(
            id = row[XroomBlockTable.id].value,
            xroomId = row[XroomBlockTable.xroomId],
            item = XroomBlockItem.valueOf(row[XroomBlockTable.item]),
            positionX = row[XroomBlockTable.positionX].toInt(),
            positionY = row[XroomBlockTable.positionY].toInt(),
            rotation = row[XroomBlockTable.rotation],
            content = row[LongTextBlockTable.content],
        )
    }
}
