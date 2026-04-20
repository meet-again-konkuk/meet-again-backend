package com.konkuk.ma.domain.xroom.domain.block

class LongTextBlock(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val content: String,
) : XroomBlock(id, xroomId, item, positionX, positionY, rotation) {
    override val type: XroomBlockType = XroomBlockType.LONG_TEXT
}
