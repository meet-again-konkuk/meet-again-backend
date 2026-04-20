package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.block.PhotoBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import java.time.LocalDate

class PhotoBlockEntity(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val photoDate: LocalDate?,
) : XroomBlockEntity(id, xroomId, item, positionX, positionY, rotation) {
    override fun toDomain(): XroomBlock {
        return PhotoBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            photoDate = photoDate,
        )
    }
}
