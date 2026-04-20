package com.konkuk.ma.domain.xroom.domain.block

import java.time.LocalDate

class PhotoBlock(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val photoDate: LocalDate?,
) : XroomBlock(id, xroomId, item, positionX, positionY, rotation) {
    override val type: XroomBlockType = XroomBlockType.PHOTO
}
