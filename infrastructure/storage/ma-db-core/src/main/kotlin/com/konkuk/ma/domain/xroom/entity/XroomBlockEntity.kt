package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.block.XroomBlock
import com.konkuk.ma.domain.xroom.domain.block.XroomBlockItem

sealed class XroomBlockEntity(
    val id: Long,
    val xroomId: Long,
    val item: XroomBlockItem,
    val positionX: Int,
    val positionY: Int,
    val rotation: Int,
) {
    abstract fun toDomain(): XroomBlock
}
