package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.exception.InvalidValueException

class NewMusicBlock(
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val musicUrl: String,
    val title: String,
    val artist: String?,
) : NewXroomBlock(xroomId, item, positionX, positionY, rotation) {

    override val type: XroomBlockType = XroomBlockType.MUSIC

    init {
        validateItemCompatibility()
        if (musicUrl.isBlank()) {
            throw InvalidValueException(NewMusicBlock::class, musicUrl, "musicUrl은 비어있을 수 없습니다.")
        }
        if (title.isBlank()) {
            throw InvalidValueException(NewMusicBlock::class, title, "title은 비어있을 수 없습니다.")
        }
    }
}
