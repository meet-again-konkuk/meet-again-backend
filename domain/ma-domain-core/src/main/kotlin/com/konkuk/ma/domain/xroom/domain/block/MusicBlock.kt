package com.konkuk.ma.domain.xroom.domain.block

class MusicBlock(
    id: Long,
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val musicUrl: String,
    val title: String,
    val artist: String?,
) : XroomBlock(id, xroomId, item, positionX, positionY, rotation) {
    override val type: XroomBlockType = XroomBlockType.MUSIC
}
