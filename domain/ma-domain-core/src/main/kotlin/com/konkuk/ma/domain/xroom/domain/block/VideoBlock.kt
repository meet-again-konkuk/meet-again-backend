package com.konkuk.ma.domain.xroom.domain.block

class VideoBlock(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val description: String?,
) : XroomBlock(id, xroomId, item, positionX, positionY, rotation) {
    override val type: XroomBlockType = XroomBlockType.VIDEO
}
