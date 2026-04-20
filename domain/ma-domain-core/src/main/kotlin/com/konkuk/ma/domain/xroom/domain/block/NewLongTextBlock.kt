package com.konkuk.ma.domain.xroom.domain.block

class NewLongTextBlock(
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val content: String,
) : NewXroomBlock(xroomId, item, positionX, positionY, rotation) {

    override val type: XroomBlockType = XroomBlockType.LONG_TEXT

    init {
        validateItemCompatibility()
        item.validateTextLength(content)
    }
}
