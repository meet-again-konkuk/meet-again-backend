package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.block.ShortTextBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem

class ShortTextBlockEntity(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val content: String,
) : XroomBlockEntity(id, xroomId, item, positionX, positionY, rotation) {
    override fun toDomain(): XroomBlock {
        return ShortTextBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            content = content,
        )
    }
}
