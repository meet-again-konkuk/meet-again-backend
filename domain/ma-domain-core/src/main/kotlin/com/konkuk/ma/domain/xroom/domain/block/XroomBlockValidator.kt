package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockPhotoQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockVideoQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.exception.InvalidValueException
import org.springframework.stereotype.Component

@Component
class XroomBlockValidator(
    private val xroomQueryRepository: XroomQueryRepository,
    private val xroomBlockPhotoQueryRepository: XroomBlockPhotoQueryRepository,
    private val xroomBlockVideoQueryRepository: XroomBlockVideoQueryRepository,
) {

    fun validate(newBlock: NewXroomBlock) {
        validatePositionRange(newBlock.positionX, "positionX")
        validatePositionRange(newBlock.positionY, "positionY")
    }

    fun validateBlockOwnership(block: XroomBlock, email: String) {
        val xroom = xroomQueryRepository.findOne(block.xroomId)
        xroom.validateOwnership(Email(email))
    }

    fun validatePhotosNotUploaded(blockId: Long) {
        if (xroomBlockPhotoQueryRepository.exists(blockId)) {
            throw DuplicateException(EntityType.XROOM_BLOCK_PHOTO, "blockId", blockId.toString())
        }
    }

    fun validateVideosNotUploaded(blockId: Long) {
        if (xroomBlockVideoQueryRepository.exists(blockId)) {
            throw DuplicateException(EntityType.XROOM_BLOCK_VIDEO, "blockId", blockId.toString())
        }
    }

    private fun validatePositionRange(position: Int, fieldName: String) {
        if (position !in MIN_POSITION..MAX_POSITION) {
            throw InvalidValueException(
                XroomBlockValidator::class,
                position,
                "${fieldName}는 $MIN_POSITION~$MAX_POSITION 범위여야 합니다."
            )
        }
    }

    companion object {
        private const val MIN_POSITION = 1
        private const val MAX_POSITION = 255
    }
}
