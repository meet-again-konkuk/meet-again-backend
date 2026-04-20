package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.exception.InvalidValueException

enum class XroomBlockItem(
    val compatibleType: XroomBlockType,
    val maxPhotoCount: Int? = null,
    val maxTextLength: Int? = null,
    val maxVideoCount: Int? = null,
) {
    POLAROID_FRAME(XroomBlockType.PHOTO, maxPhotoCount = 1),
    COLLAGE_3(XroomBlockType.PHOTO, maxPhotoCount = 3),
    GALLERY_WALL(XroomBlockType.PHOTO, maxPhotoCount = 10),

    PLAIN_CARD(XroomBlockType.SHORT_TEXT, maxTextLength = 200),

    LETTER_PAPER(XroomBlockType.LONG_TEXT, maxTextLength = 2000),
    LONG_LETTER(XroomBlockType.LONG_TEXT, maxTextLength = 10000),

    MUSIC_CARD(XroomBlockType.MUSIC),

    DDAY_BADGE(XroomBlockType.DDAY),

    VIDEO_FRAME(XroomBlockType.VIDEO, maxVideoCount = 1),
    ;

    fun validateCompatibility(blockType: XroomBlockType) {
        if (compatibleType != blockType) {
            throw InvalidValueException(
                XroomBlockItem::class,
                this,
                "$name 아이템은 $compatibleType 타입에만 사용할 수 있습니다 (요청: $blockType)."
            )
        }
    }

    fun validateTextLength(text: String) {
        val limit = requireMaxTextLength()
        if (text.length > limit) {
            throw InvalidValueException(
                XroomBlockItem::class,
                text.length,
                "$name 아이템의 최대 텍스트 길이는 ${limit}자입니다."
            )
        }
    }

    fun validatePhotoCount(uploadCount: Int) {
        val limit = requireMaxPhotoCount()
        if (uploadCount != limit) {
            throw InvalidValueException(
                XroomBlockItem::class,
                uploadCount,
                "$name 아이템은 정확히 ${limit}장의 사진이 필요합니다."
            )
        }
    }

    fun validateVideoCount(uploadCount: Int) {
        val limit = requireMaxVideoCount()
        if (uploadCount != limit) {
            throw InvalidValueException(
                XroomBlockItem::class,
                uploadCount,
                "$name 아이템은 정확히 ${limit}개의 영상이 필요합니다."
            )
        }
    }

    private fun requireMaxTextLength(): Int {
        return maxTextLength
            ?: throw InvalidValueException(XroomBlockItem::class, this, "$name 아이템은 텍스트 길이 제한이 정의되지 않았습니다.")
    }

    private fun requireMaxPhotoCount(): Int {
        return maxPhotoCount
            ?: throw InvalidValueException(XroomBlockItem::class, this, "$name 아이템은 사진 수 제한이 정의되지 않았습니다.")
    }

    private fun requireMaxVideoCount(): Int {
        return maxVideoCount
            ?: throw InvalidValueException(XroomBlockItem::class, this, "$name 아이템은 영상 수 제한이 정의되지 않았습니다.")
    }
}
