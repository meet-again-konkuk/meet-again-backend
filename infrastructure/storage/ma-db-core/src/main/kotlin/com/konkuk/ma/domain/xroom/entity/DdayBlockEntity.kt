package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.block.DdayBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem
import java.time.LocalDate

class DdayBlockEntity(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val anniversaryDate: LocalDate,
    val label: String,
) : XroomBlockEntity(id, xroomId, item, positionX, positionY, rotation) {
    override fun toDomain(): XroomBlock {
        return DdayBlock(
            id = id,
            xroomId = xroomId,
            item = item,
            positionX = positionX,
            positionY = positionY,
            rotation = rotation,
            anniversaryDate = anniversaryDate,
            label = label,
        )
    }
}
