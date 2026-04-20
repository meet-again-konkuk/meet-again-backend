package com.konkuk.ma.domain.xroom.domain.block

sealed class NewXroomBlock(
    val xroomId: Long,
    val item: XroomBlockItem,
    val positionX: Int,
    val positionY: Int,
    val rotation: Int,
) {
    abstract val type: XroomBlockType

    protected fun validateItemCompatibility() {
        item.validateCompatibility(type)
    }
}
