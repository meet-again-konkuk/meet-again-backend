package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.block.MusicBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem

class MusicBlockEntity(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val musicUrl: String,
    val title: String,
    val artist: String?,
) : XroomBlockEntity(id, xroomId, item, positionX, positionY, rotation) {
    override fun toDomain(): XroomBlock {
        return MusicBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            musicUrl = musicUrl,
            title = title,
            artist = artist,
        )
    }
}
