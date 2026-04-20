package com.konkuk.ma.domain.xroom.domain.block

import java.time.LocalDate

class NewPhotoBlock(
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val photoDate: LocalDate?,
) : NewXroomBlock(xroomId, item, positionX, positionY, rotation) {

    override val type: XroomBlockType = XroomBlockType.PHOTO

    init {
        validateItemCompatibility()
    }
}
