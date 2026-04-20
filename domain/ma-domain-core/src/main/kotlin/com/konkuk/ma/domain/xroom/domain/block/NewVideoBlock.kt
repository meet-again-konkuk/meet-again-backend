package com.konkuk.ma.domain.xroom.domain.block

class NewVideoBlock(
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val description: String?,
) : NewXroomBlock(xroomId, item, positionX, positionY, rotation) {

    override val type: XroomBlockType = XroomBlockType.VIDEO

    init {
        validateItemCompatibility()
    }
}
