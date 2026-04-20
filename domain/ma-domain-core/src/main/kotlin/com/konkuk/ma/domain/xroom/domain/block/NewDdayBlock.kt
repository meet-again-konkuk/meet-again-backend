package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.exception.InvalidValueException
import java.time.LocalDate

class NewDdayBlock(
    xroomId: Long,
    item: XroomBlockItem,
    positionX: Int,
    positionY: Int,
    rotation: Int,
    val anniversaryDate: LocalDate,
    val label: String,
) : NewXroomBlock(xroomId, item, positionX, positionY, rotation) {

    override val type: XroomBlockType = XroomBlockType.DDAY

    init {
        validateItemCompatibility()
        if (label.isBlank()) {
            throw InvalidValueException(NewDdayBlock::class, label, "label은 비어있을 수 없습니다.")
        }
    }
}
