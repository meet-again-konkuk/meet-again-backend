package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockType
import com.konkuk.ma.domain.xroom.entity.MusicBlockEntity
import com.konkuk.ma.domain.xroom.entity.XroomBlockEntity
import com.konkuk.ma.domain.xroom.entity.table.MusicBlockTable
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.springframework.stereotype.Component

@Component
class MusicBlockEntityFactory : XroomBlockEntityFactory {
    override val type: XroomBlockType = XroomBlockType.MUSIC
    override val childTable: LongIdTable = MusicBlockTable

    override fun createFrom(row: ResultRow): XroomBlockEntity {
        return MusicBlockEntity(
            id = row[XroomBlockTable.id].value,
            xroomId = row[XroomBlockTable.xroomId],
            item = XroomBlockItem.valueOf(row[XroomBlockTable.item]),
            positionX = row[XroomBlockTable.positionX].toInt(),
            positionY = row[XroomBlockTable.positionY].toInt(),
            rotation = row[XroomBlockTable.rotation],
            musicUrl = row[MusicBlockTable.musicUrl],
            title = row[MusicBlockTable.title],
            artist = row[MusicBlockTable.artist],
        )
    }
}
