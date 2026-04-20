package com.konkuk.ma.domain.xroom.domain.block

import java.time.LocalDate

class DdayBlock(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val anniversaryDate: LocalDate,
    val label: String,
) : XroomBlock(id, xroomId, item, positionX, positionY, rotation) {
    override val type: XroomBlockType = XroomBlockType.DDAY
}
