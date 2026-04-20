package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.exception.InvalidValueException

sealed class XroomBlock(
    val id: Long,
    val xroomId: Long,
    val item: XroomBlockItem,
    val positionX: Int,
    val positionY: Int,
    val rotation: Int,
) {
    abstract val type: XroomBlockType

    fun validateTypeIs(expected: XroomBlockType) {
        if (type != expected) {
            throw InvalidValueException(
                XroomBlock::class,
                type,
                "$expected 타입 블록이 아닙니다 (실제: $type)."
            )
        }
    }

    fun validatePhotoCount(uploadCount: Int) {
        validateTypeIs(XroomBlockType.PHOTO)
        item.validatePhotoCount(uploadCount)
    }

    fun validateVideoCount(uploadCount: Int) {
        validateTypeIs(XroomBlockType.VIDEO)
        item.validateVideoCount(uploadCount)
    }
}
